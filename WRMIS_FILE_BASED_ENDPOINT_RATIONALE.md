# Why WRMIS Endpoints Must Read Directly from Files
## (Not from the Database for Legacy/CSV Data)

---

## 1. The `current_licence_id` Circular Link Can Never Exist for Legacy Data

`CoreLicenseApplication` has a `@ManyToOne` to `CoreLicense` via `current_licence_id`.
`CoreLicense` has a `@ManyToOne` back to `CoreLicenseApplication` via `license_application_id`.

In the normal system process, `current_licence_id` is set on the application **only after** the
license has been formally issued at the end of the full approval workflow. Legacy CSV records were
never processed through that workflow — they existed before the system. So `current_licence_id`
is permanently null on every imported record.

The WRMIS query depends on this exact join:
```sql
left join core_license cl1_0 on cl1_0.id = cla1_0.current_licence_id
```
With `current_licence_id` null, every license field (`cl1_0.license_number`,
`cl1_0.expiration_date`, `cl1_0.status`, etc.) comes back null for all legacy records.
The WRMIS approved permits response is effectively empty for all historical data.

---

## 2. `application_step_id` Requires a Workflow Journey That Never Happened

`CoreApplicationStep` has a `sequence_number` and belongs to a `CoreLicenseType` via
`license_type_id`. An application's `application_step_id` is only set as it moves through
workflow stages (submission → officer review → assessment → board → DRS → approval).

Legacy records skipped this entire journey. `application_step_id` is null on all of them.

The WRMIS query joins:
```sql
left join core_application_step cas2_0 on cas2_0.id = cla1_0.application_step_id
left join core_license_type clt1_0 on clt1_0.id = cas2_0.license_type_id
```
With `application_step_id` null, `clt1_0` (license type resolved via step) is always null.
The `WRMISPermitApplicationDTO.licenseType` field is null for all legacy records even though
the license type data exists in the DB — it just cannot be reached through this join path.

---

## 3. `CoreLicenseAssessment` Does Not Exist for Legacy Records — Volume Is Always Null

In `WRMISDataServiceImpl.mapToApprovedPermitDTO()`, the approved volume is sourced from:
```java
CoreLicenseAssessment assessment = assessmentMap.get(app.getId());
if (assessment != null && assessment.getCalculatedAnnualRental() != null) {
    dto.setApprovedVolume(assessment.getCalculatedAnnualRental());
}
```
`CoreLicenseAssessment` is created by a licensed officer during the assessment stage of the
workflow. It holds `rental_quantity`, `rental_rate`, and `calculated_annual_rental` — all
`nullable` in the schema. Legacy records have no assessment record at all. So `approvedVolume`
is always null in the WRMIS response for every imported record, even though the CSV has the
volume data sitting right there in the `Volume (M3/Day)` column.

---

## 4. `applicationType` Is Null — The System Cannot Classify the Record

`CoreLicenseApplication.applicationType` distinguishes `NEW`, `RENEWAL`, `TRANSFER`,
`VARIATION`. Each type drives a different workflow path, fee structure, document checklist,
and officer assignment. The CSV has no such column — all four Excel sheets represent different
permit categories (Surface Water, Drillers, Unlicensed, Effluent) but none map to
`applicationType`. All imported records land with `applicationType = null`.

In `WRMISDataServiceImpl.mapToPermitApplicationDTO()` this is patched with:
```java
dto.setApplicationType(app.getApplicationType() != null ? app.getApplicationType() : "NEW");
```
So WRMIS receives `"NEW"` for every single legacy record — renewals, transfers, and variations
all appear as new applications. This is factually wrong data being sent to WRMIS.

---

## 5. `owner_id` Is `nullable = false` but Semantically Wrong for Legacy Records

`CoreLicenseApplication.owner_id` is declared `nullable = false` and in the normal process
refers to the **NWRA officer** who owns/handles the application internally. The import script
sets `owner_id` to the applicant's generated user ID because there is no officer in the CSV.

`SysUserAccount` has `user_group_id` linking to `SysUserGroup` which determines whether the
account is an applicant or an officer. The imported user accounts are created with the applicant
user group (`308419ea-e8a5-11ef-b39b-c84bd6560e35`). Any officer dashboard query filtering
applications by `owner_id` will surface these legacy records as if they belong to applicant
accounts, not officers — corrupting officer workload views.

---

## 6. `license_type_id` Is Set to `LIMIT 1` — All Legacy Records Get the Same Type

The import script uses:
```sql
SELECT id FROM core_license_type LIMIT 1
```
This assigns the same first license type to every imported record regardless of category.
The Excel file has four distinct sheets:
- `All drillers` / `Active drillers` → Drilling permits
- `unlicensed applications` → Surface/Ground water permits
- `Effluent Category` → Effluent discharge permits

All collapse into one license type in the DB. The WRMIS `licenseType` field in
`WRMISApprovedPermitDTO` will return the same type for a borehole driller and an effluent
discharger — making license type filtering and reporting at the WRMIS side meaningless.

---

## 7. `CoreLicenseWaterUse` Links to `core_water_use` via FK — Creates Duplicate Reference Data

`CoreLicenseWaterUse` has `@ManyToOne @JoinColumn(name = "water_use_id")` to `CoreWaterUse`.
In the normal process, the applicant selects from a predefined list. The import creates new
`core_water_use` entries on the fly from free-text CSV values like `"Domestic "`, `"domestic"`,
`"Public"`, `"Irrigation"` — with inconsistent spacing and casing.

The DB ends up with multiple `core_water_use` rows for the same concept. Any WRMIS query
grouping or filtering by water use type now produces fragmented counts — `"Domestic"` and
`"Domestic "` are treated as two different use types.

---

## 8. `dest_*` Fields Are Entirely Null — Half the Application Schema Is Empty

`CoreLicenseApplication` has a full set of destination fields:
`dest_easting`, `dest_northing`, `dest_ta`, `dest_village`, `dest_owner_fullname`,
`dest_plot_number`, `dest_land_regime_id`, `dest_wru`, `dest_hactarage`.

These are required for diversion and irrigation permits where water is taken from a source
and delivered to a destination. The CSV only has source coordinates (`X`, `Y`). All
`dest_*` fields are null for every imported record. WRMIS receives incomplete spatial data
for permit types where destination is mandatory.

---

## 9. `source_wru` and `dest_wru` Are Null — Records Are Invisible to Basin-Level Queries

`CoreLicenseApplication` has `@ManyToOne @JoinColumn(name = "source_wru")` and
`@ManyToOne @JoinColumn(name = "dest_wru")` linking to `CoreWaterResourceUnit`. These FKs
are how the system organizes permits by water catchment and basin. Without them, all imported
records are invisible to any basin-level allocation query or water resource unit report that
WRMIS runs. The spatial hierarchy (WRU → Water Resource Area → District) is completely broken
for all legacy data.

---

## 10. `SysUserAccount` Has No `password` or `account_status_id` Integrity for Imported Users

The import creates `sys_user_account` rows with a hardcoded `account_status_id`
(`be9cdb78-b501-11eb-a683-94659cc92a92`) and no password. `SysUserAccount` has
`@ManyToOne @JoinColumn(name = "account_status_id")` to `SysAccountStatus`. If that UUID
does not exist in `sys_account_status`, Hibernate resolves the join to null — meaning
`app.getSysUserAccount().getSysAccountStatus()` returns null, and any code checking account
status before processing will either throw a NullPointerException or silently skip the record.

---

## 11. `dateSubmitted` vs `dateCreated` — WRMIS Date Queries Return Wrong Results

`BaseEntity.date_created` is set to `datetime.now()` at import time (today's date).
`CoreLicenseApplication.date_submitted` is set to the registration date from the CSV
(which can be from 1971 to 2024). The WRMIS service queries by `date_submitted`:
```java
applicationRepository.findByDateSubmittedBetween(dateFrom, dateTo)
```
But `getApprovedPermitsByDate()` filters `CoreLicense` by `date_issued` which was also
set from the CSV registration date. This means a query for `date=2025-03-25` (as seen in
the error) returns zero results because no legacy record has a `date_issued` of 2025-03-25
— they all have historical dates. The endpoint appears broken when it is actually working
correctly against wrong data.

---

## Conclusion

Reading directly from the files for these WRMIS endpoints means:
- The data is served exactly as it exists in the source files with no schema constraint gaps
- No null joins, no missing assessments, no wrong license types, no circular link failures
- The DB remains clean for records that went through the proper workflow
- WRMIS gets accurate historical data without the system trying to force legacy records
  through a schema designed for a live workflow process
