# NWRA eWater Permit System — BomaPay Payment Integration
## What Payment Integration Actually Means (It Is NOT Plug and Play)

> **System:** National Water Resources Authority (NWRA) eWater Permit System
> **Payment Gateway:** BomaPay (BPC Malawi)
> **Currency:** MWK (Malawi Kwacha) — ISO 4217 Code: `454`
> **Environment:** Dev → `https://dev.bpcbt.com/payment`

---

## The Myth vs The Reality

| What People Think | What It Actually Is |
|---|---|
| "Just add a payment button" | 9+ distinct integration layers |
| "The gateway handles everything" | We handle transaction audit, state machine, notifications, IFMIS posting |
| "It's done when money moves" | Money moving is step 4 of 9 |
| "One API call" | Multiple API calls: register → redirect → poll/webhook → verify → process → post |
| "A few hours of work" | Weeks of engineering across frontend, backend, database, and government systems |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        APPLICANT (Browser)                       │
└────────────────────────────┬────────────────────────────────────┘
                             │ 1. Clicks Pay
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                   NWRA Backend (Spring Boot)                     │
│                                                                  │
│  PaymentServiceImpl          BomaPayServiceImpl                  │
│  ├─ initiatePayment()        ├─ initiatePayment()                │
│  ├─ verifyPayment()          ├─ getPaymentStatus()               │
│  ├─ processApplicationPayment├─ handleWebhook()                  │
│  ├─ generateInvoice()        ├─ reversePayment()                 │
│  ├─ confirmPayment()         └─ processCompletedPayment()        │
│  ├─ registerBOMAPIayOrder()                                      │
│  ├─ getBOMAPIayOrderStatus() BomaPayTransactionHistoryService    │
│  ├─ processMobileMoneyPayment├─ savePaymentInitiation()          │
│  └─ refundBOMAPIayOrder()    ├─ updatePaymentStatus()            │
│                              ├─ markPaymentCompleted()           │
│                              ├─ recordWebhookReceived()          │
│                              └─ recordReturnUrlVisited()         │
└────────────────────────────┬────────────────────────────────────┘
                             │ 2. Register Order
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    BomaPay Gateway                               │
│           https://dev.bpcbt.com/payment                         │
│                                                                  │
│  POST /rest/register.do          → Get orderId + formUrl        │
│  POST /rest/getOrderStatusExtended.do → Poll status             │
│  POST /rest/reverse.do           → Reverse a payment            │
│  POST /rest/mobilePayment.do     → Mobile money                 │
│  POST /rest/refund.do            → Refund a payment             │
│  WEBHOOK → POST to our server    → Real-time status push        │
└────────────────────────────┬────────────────────────────────────┘
                             │ 3. Redirect to Hosted Page
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│              BomaPay Hosted Payment Page (formUrl)              │
│         Applicant enters card / mobile money details            │
└────────────────────────────┬────────────────────────────────────┘
                             │ 4. Payment Processed
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│              IFMIS (Government Accounting System)               │
│           National Payment Gateway (NPG) Interface              │
│                                                                  │
│  BLDAT  = Document Date        BUDAT = Posting Date             │
│  BLART  = Document Type        BUKRS = Company Code             │
│  MONAT  = Accounting Period    WAERS = Currency                 │
│  XBLNR  = Reference Number     GSBER = Business Area           │
│  GEBER  = Fund                 GRANT_NBR = Grant               │
│  FKBER  = Functional Area      FIPEX = Commitment Item         │
│  Debit Entry (NEWBS/NEWKO/WRBTR)                                │
│  Credit Entry (NEWBS/NEWKO/WRBTR/MWSKZ/SGTXT)                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## The 9 Layers of Payment Integration

### Layer 1: Payment Initiation
**File:** [BomaPayServiceImpl.java](src/main/java/mw/nwra/ewaterpermit/service/BomaPayServiceImpl.java#L63)

What must happen before a single payment request is sent:
- Validate the `CoreLicenseApplication` exists and is in correct state
- Determine `paymentType` → `APPLICATION_FEE` or `LICENSE_FEE`
- Generate a **unique, deterministic** `orderNumber` per application + type + timestamp
  ```
  Format: NWRA-{paymentType}-{applicationId}-{yyyyMMddHHmmss}
  Example: NWRA-LICENSE_FEE-a1b2c3d4-20260223143022
  ```
- Convert amount to **minor units** (MWK × 100) — BomaPay requires this
  ```java
  Long amountInMinorUnits = Math.round(amount * 100);
  // MWK 10,000 becomes 1000000 in the request
  ```
- Build and send form-urlencoded POST (not JSON) to `/rest/register.do`
- Parse response — check **both** `errorCode == "0"` AND presence of `orderId` + `formUrl`
- Handle **three** distinct failure paths: API error, business error, exception

---

### Layer 2: Transaction Audit Trail
**File:** [BomaPayTransactionHistoryServiceImpl.java](src/main/java/mw/nwra/ewaterpermit/service/BomaPayTransactionHistoryServiceImpl.java)

Every payment attempt — successful or not — must be recorded **before** we know the outcome:

```
savePaymentInitiation()
  ├─ orderId (BomaPay's reference)
  ├─ orderNumber (our internal reference: NWRA-...)
  ├─ amount + currency
  ├─ paymentType
  ├─ formUrl (hosted payment page URL)
  ├─ initiatedByUserId
  ├─ initiatedDate
  ├─ coreLicenseApplication (FK)
  ├─ webhookReceived = false
  └─ returnUrlVisited = false
```

This serves as:
- Audit log for finance team
- Idempotency guard (prevent double-charging)
- Reconciliation source for failed/pending payments
- Evidence for disputes and reversals

---

### Layer 3: Hosted Payment Redirect
**File:** [BomaPayServiceImpl.java](src/main/java/mw/nwra/ewaterpermit/service/BomaPayServiceImpl.java#L131)

This system uses the **hosted payment approach** — we do NOT handle card details ourselves. This is a deliberate security decision:

- BomaPay returns a `formUrl` — a one-time hosted payment page URL
- Our backend returns this URL to the frontend
- Frontend redirects the applicant to BomaPay's page
- Applicant enters card / mobile money details on BomaPay's servers
- We never see PAN (Primary Account Number) or CVV — **PCI DSS compliance**

This requires:
- `returnUrl` — where BomaPay sends applicant after success
  ```
  http://localhost:4200/payments/return
  ```
- `failUrl` — where BomaPay sends applicant after failure
  ```
  http://localhost:4200/payments/failed
  ```
- These URLs must be **environment-specific** (dev vs production)
- Frontend must handle both return and fail URL flows

---

### Layer 4: BomaPay Status Code Mapping
**File:** [BomaPayServiceImpl.java](src/main/java/mw/nwra/ewaterpermit/service/BomaPayServiceImpl.java#L504)

BomaPay returns numeric codes. We maintain our own status vocabulary:

| BomaPay Code | BomaPay Meaning | Our Status |
|---|---|---|
| `0` | Order registered | `REGISTERED` |
| `1` | Processing / Pre-auth hold | `PROCESSING` |
| `2` | Deposited / Completed | `COMPLETED` |
| `3` | Reversed | `REVERSED` |
| `4` | Refunded | `REFUNDED` |
| `5` | ACS Redirect (3DS auth) | `ACS_REDIRECT` |
| `6` | Declined | `DECLINED` |

This mapping must be maintained in sync with BomaPay API changes. Each status drives different behaviour in our system (notifications, application state, workflows).

---

### Layer 5: Dual Notification Strategy
**File:** [BomaPayServiceImpl.java](src/main/java/mw/nwra/ewaterpermit/service/BomaPayServiceImpl.java#L517)

Three notification events per payment lifecycle:

```
PAYMENT INITIATED
  → Title: "Payment Initiated"
  → Type: INFO
  → Priority: MEDIUM
  → Category: PAYMENT
  → Action: /e-services/my-applications

PAYMENT SUCCESSFUL
  → Title: "Payment Successful!"
  → Type: SUCCESS
  → Priority: HIGH
  → Category: PAYMENT
  → Message: "Your {type} payment of MWK {amount} for {licenseType}
              has been successfully processed. Order: {orderNumber}"

PAYMENT FAILED (DECLINED or REVERSED)
  → Title: "Payment Failed"
  → Type: ERROR
  → Priority: HIGH
  → Category: PAYMENT
  → Action: /e-services/my-applications (Try Again)
```

Notifications must be triggered from:
- Status check polling (`getPaymentStatus`)
- Webhook handler (`handleWebhook`)
- Both paths must produce identical notifications to avoid duplicates

---

### Layer 6: Webhook Handler (Real-Time Push)
**File:** [BomaPayServiceImpl.java](src/main/java/mw/nwra/ewaterpermit/service/BomaPayServiceImpl.java#L355)

BomaPay pushes payment outcomes to our server asynchronously. This is critical:

```
BomaPay → POST /api/bomapay/webhook
           {
             "orderId": "...",
             "orderNumber": "NWRA-...",
             "status": "2"
           }
```

Our webhook handler must:
1. Record that webhook was received (`recordWebhookReceived`)
2. Map the incoming status to our status vocabulary
3. Update transaction history
4. If `COMPLETED`: mark payment complete + trigger notifications
5. If `DECLINED` / `REVERSED`: trigger failure notifications
6. Return HTTP 200 quickly — BomaPay will retry on failure

**This is NOT the same as the return URL.** A user could close the browser before returning — the webhook is the only reliable confirmation.

Pending integration (currently TODO in code):
```java
// TODO: Create CoreApplicationPayment record
// TODO: Update application status
// TODO: Trigger approval workflow
// TODO: Send confirmation emails
// TODO: Replicate upload-receipt approval behaviour
```

---

### Layer 7: Status Polling (Fallback)
**File:** [BomaPayServiceImpl.java](src/main/java/mw/nwra/ewaterpermit/service/BomaPayServiceImpl.java#L212)

Webhooks can fail (network issues, downtime). We must also support polling:

```
POST /rest/getOrderStatusExtended.do
  ├─ userName, password, orderId
  └─ Returns: orderStatus, amount, currency, paymentAmountInfo
```

Polling is triggered when:
- User returns to `returnUrl` (we check the actual status, not just assume success)
- Admin manually checks a stalled payment
- Nightly reconciliation job (to be built)
- Any "pending" transaction older than N minutes

---

### Layer 8: Payment Reversal
**File:** [BomaPayServiceImpl.java](src/main/java/mw/nwra/ewaterpermit/service/BomaPayServiceImpl.java#L444)

Payments must be reversible (admin error, application withdrawal, duplicate charge):

```
POST /rest/reverse.do
  ├─ userName, password, orderId
  └─ Returns: errorCode 0 = success
```

Reversal is different from refund — it cancels before settlement. After reversal:
- Transaction history updated to `REVERSED`
- Application payment record must be voided
- Notifications sent to applicant
- Audit trail preserved (cannot delete history)

---

### Layer 9: IFMIS Government Accounting Integration
**File:** [PaymentServiceImpl.java](src/main/java/mw/nwra/ewaterpermit/service/PaymentServiceImpl.java#L224)

Every payment collected by NWRA must post to Malawi's government accounting system (IFMIS) via the National Payment Gateway (NPG) interface. This is not optional — it is a government financial compliance requirement.

Each payment request carries full double-entry bookkeeping fields:

```
NPG Interface Fields:
  BLDAT  → Document Date (when transaction occurred)
  BUDAT  → Posting Date (when posted to books)
  BLART  → Document Type (SA, ZP, etc.)
  BUKRS  → Company Code (NWRA's code in IFMIS)
  MONAT  → Accounting Period (01-12)
  WAERS  → Currency (MWK = 454)
  XBLNR  → External Reference (our NWRA-... number)
  GSBER  → Business Area
  GEBER  → Fund
  GRANT_NBR → Grant (donor funding if applicable)
  FKBER  → Functional Area
  FIPEX  → Commitment Item

  Debit Entry:
    NEWBS  → Posting Key (Debit side)
    NEWKO  → G/L Account (Debit)
    WRBTR  → Amount

  Credit Entry:
    NEWBS  → Posting Key (Credit side)
    NEWKO  → Revenue G/L Account (Credit)
    WRBTR  → Revenue Amount
    MWSKZ  → Tax Code
    SGTXT  → Item Text / Description
```

This means our system must know:
- NWRA's chart of accounts (revenue GL codes per permit type)
- The correct posting keys for debit/credit
- The relevant fund, business area, grant per transaction
- The IFMIS document type for water permit fees

---

## Fee Structure Configured in System

**File:** [PaymentServiceImpl.java](src/main/java/mw/nwra/ewaterpermit/service/PaymentServiceImpl.java#L263)

| Fee Type | Amount (MWK) |
|---|---|
| APPLICATION_FEE | 5,000.00 |
| PERMIT_FEE | 10,000.00 |
| SURFACE_WATER_PERMIT | 5,000.00 |
| EFFLUENT_DISCHARGE_PERMIT | 7,500.00 |
| BOREHOLE_PERMIT | 6,000.00 |
| RENEWAL_FEE | 3,000.00 |
| TRANSFER_FEE | 2,500.00 |
| VARIATION_FEE | 2,000.00 |

These amounts currently live in code. They should move to a database configuration table so finance team can adjust without a deployment.

---

## Payment Methods Supported

| Method | API Endpoint | Status |
|---|---|---|
| Card (Hosted Page) | `/rest/register.do` → redirect | Implemented |
| Mobile Money | `/rest/mobilePayment.do` | Implemented |
| Refund | `/rest/refund.do` | Implemented |
| Reversal | `/rest/reverse.do` | Implemented |

---

## What Is Still Pending (Current TODOs in Code)

These are explicitly marked as `TODO: INTEGRATION POINT` in the source:

### 1. Link Completed BomaPay Payment → Application Record
When BomaPay confirms payment (`COMPLETED`), we must:
- Create `CoreApplicationPayment` record in our database
- Link it to the `CoreLicense` and `CoreLicenseApplication`
- Update application status (if payment unlocks next workflow stage)

### 2. Trigger Application Approval Workflow
Currently the upload-receipt approval workflow exists for manual receipts.
When BomaPay payment completes, the same approval path must be triggered automatically — no human receipt upload required.

### 3. Send Email Confirmation
In-app notifications are implemented. Email confirmation to applicant after payment success is pending.

### 4. Nightly Reconciliation Job
A scheduled job is needed to:
- Find all `REGISTERED` or `PROCESSING` transactions older than 30 minutes
- Poll BomaPay for their actual status
- Update records accordingly
- Flag any discrepancies for finance team review

### 5. Production Environment Configuration
Current config points to dev gateway. Before go-live:
- Production BomaPay credentials (`application.properties`)
- Production `returnUrl` / `failUrl` (not `localhost:4200`)
- Production IFMIS GL codes and posting keys
- SSL/TLS certificate validation (dev may skip this)
- Webhook IP whitelisting (BomaPay's server IP must be allowed)

---

## Configuration Properties Required

```properties
# BomaPay Gateway
bomapay.base.url=https://dev.bpcbt.com/payment
bomapay.username=test_user
bomapay.password=test_user_password
bomapay.currency=454
bomapay.client.id=259753456
bomapay.merchant.login=OurBestMerchantLogin
bomapay.return.url=http://localhost:4200/payments/return
bomapay.fail.url=http://localhost:4200/payments/failed

# These must change per environment (dev/staging/production)
```

---

## Payment Lifecycle — Full State Machine

```
                    ┌─────────────┐
                    │  INITIATED  │ ← User clicks Pay
                    └──────┬──────┘
                           │ register.do → orderId returned
                           ▼
                    ┌─────────────┐
                    │  REGISTERED │ ← Saved to BomaPayTransactionHistory
                    └──────┬──────┘
                           │ User redirected to formUrl (hosted page)
                           ▼
                    ┌─────────────┐
                    │ PROCESSING  │ ← User enters card/mobile money
                    └──────┬──────┘
                           │
              ┌────────────┴────────────┐
              ▼                         ▼
       ┌─────────────┐          ┌─────────────┐
       │  COMPLETED  │          │  DECLINED   │
       └──────┬──────┘          └──────┬──────┘
              │                        │
              │                   User retries
              ▼                        │
  ┌─────────────────────┐             ─┘
  │ CoreApplicationPayment│
  │ record created        │
  │ Approval workflow     │
  │ triggered             │
  │ Email sent            │
  │ IFMIS posted          │
  └─────────────────────┘
              │
              │ (Admin action if needed)
              ▼
       ┌─────────────┐
       │  REVERSED   │ ← reverse.do called
       └─────────────┘
              │
       ┌─────────────┐
       │  REFUNDED   │ ← refund.do called (after settlement)
       └─────────────┘
```

---

## Files Involved in Payment Integration

| File | Responsibility |
|---|---|
| [BomaPayService.java](src/main/java/mw/nwra/ewaterpermit/service/BomaPayService.java) | Interface contract — 5 core operations |
| [BomaPayServiceImpl.java](src/main/java/mw/nwra/ewaterpermit/service/BomaPayServiceImpl.java) | Full BomaPay API integration + notifications |
| [BomaPayTransactionHistoryService.java](src/main/java/mw/nwra/ewaterpermit/service/BomaPayTransactionHistoryService.java) | Audit trail interface — 10 operations |
| [BomaPayTransactionHistoryServiceImpl.java](src/main/java/mw/nwra/ewaterpermit/service/BomaPayTransactionHistoryServiceImpl.java) | Audit trail persistence |
| [PaymentServiceImpl.java](src/main/java/mw/nwra/ewaterpermit/service/PaymentServiceImpl.java) | IFMIS posting, mobile money, invoices, fee types |
| `NotificationService` | In-app notifications for all payment events |
| `CoreApplicationPaymentService` | Links payments to license applications |
| `CoreFeesTypeService` | Fee type configuration lookup |
| `BomaPayTransactionHistoryRepository` | JPA repository for transaction records |

---

## Summary: What Payment Integration Actually Involves

| Activity | Done | Pending |
|---|---|---|
| BomaPay API connection (register, status, reverse, refund, mobile) | ✅ | |
| Hosted payment page redirect | ✅ | |
| Transaction audit trail (every attempt, success, failure) | ✅ | |
| Status code mapping (BomaPay numeric → our vocabulary) | ✅ | |
| Webhook handler (real-time push from BomaPay) | ✅ | |
| Status polling fallback | ✅ | |
| In-app notifications (initiated, success, failure) | ✅ | |
| Payment reversal | ✅ | |
| IFMIS/NPG government accounting fields | ✅ | |
| Mobile money payment | ✅ | |
| Fee type configuration per permit type | ✅ | |
| Create CoreApplicationPayment on completion | | ⬜ |
| Trigger approval workflow after BomaPay payment | | ⬜ |
| Email confirmation on payment success | | ⬜ |
| Nightly reconciliation job | | ⬜ |
| Production environment configuration | | ⬜ |
| Fee amounts in database (not hardcoded) | | ⬜ |
| Webhook IP whitelisting | | ⬜ |

---

*Generated from source analysis of NWRA eWater Permit System — February 2026*
