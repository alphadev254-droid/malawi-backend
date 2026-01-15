/**
 * Frontend PDF Generation Utility for Water Allocation Reports
 * Requires jsPDF library: https://github.com/parallax/jsPDF
 * 
 * Include this in your HTML:
 * <script src="https://cdnjs.cloudflare.com/ajax/libs/jspdf/2.5.1/jspdf.umd.min.js"></script>
 * <script src="https://cdnjs.cloudflare.com/ajax/libs/jspdf-autotable/3.5.25/jspdf.plugin.autotable.min.js"></script>
 */

class WaterReportPDFGenerator {
    constructor() {
        // Check if jsPDF is available
        if (typeof window.jsPDF === 'undefined') {
            console.error('jsPDF library is required. Please include it in your HTML.');
            return;
        }
        
        this.jsPDF = window.jsPDF;
        this.pageWidth = 210; // A4 width in mm
        this.pageHeight = 297; // A4 height in mm
        this.margin = 20;
        this.headerHeight = 30;
        this.footerHeight = 20;
    }

    // Utility method to format numbers
    formatNumber(value) {
        if (value === null || value === undefined) return '0.00';
        if (typeof value === 'number') {
            return value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
        }
        try {
            return parseFloat(value).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
        } catch (e) {
            return value.toString();
        }
    }

    // Utility method to format currency
    formatCurrency(value) {
        return 'MWK ' + this.formatNumber(value);
    }

    // Add header and footer to each page
    addHeaderFooter(doc, title) {
        const pageCount = doc.internal.getNumberOfPages();
        
        for (let i = 1; i <= pageCount; i++) {
            doc.setPage(i);
            
            // Header
            doc.setFontSize(16);
            doc.setFont(undefined, 'bold');
            doc.text('National Water Resources Authority (NWRA)', this.pageWidth / 2, 15, { align: 'center' });
            
            doc.setFontSize(14);
            doc.text(title, this.pageWidth / 2, 25, { align: 'center' });
            
            // Footer
            doc.setFontSize(8);
            doc.setFont(undefined, 'normal');
            const now = new Date();
            const dateStr = now.toLocaleDateString() + ' ' + now.toLocaleTimeString();
            doc.text(`Generated on: ${dateStr}`, this.margin, this.pageHeight - 10);
            doc.text(`Page ${i} of ${pageCount}`, this.pageWidth - this.margin, this.pageHeight - 10, { align: 'right' });
        }
    }

    // 1. Water Allocation Level Report
    generateWaterAllocationLevelReport(reportData) {
        const doc = new this.jsPDF();
        const title = 'Water Allocation Level Report';
        
        let yPos = 40;
        
        // Summary section
        if (reportData.summary) {
            doc.setFontSize(12);
            doc.setFont(undefined, 'bold');
            doc.text('Summary', this.margin, yPos);
            yPos += 10;
            
            const summaryData = [
                ['Total Water Resource Areas', reportData.summary.totalAreas || 0],
                ['Total Water Resource Units', reportData.summary.totalUnits || 0],
                ['Total Water Abstraction (ML/year)', this.formatNumber(reportData.summary.totalAbstraction)]
            ];
            
            doc.autoTable({
                startY: yPos,
                head: [['Metric', 'Value']],
                body: summaryData,
                margin: { left: this.margin, right: this.margin },
                styles: { fontSize: 10 }
            });
            
            yPos = doc.lastAutoTable.finalY + 15;
        }
        
        // Allocation by area
        if (reportData.allocationByArea && reportData.allocationByArea.length > 0) {
            doc.setFontSize(12);
            doc.setFont(undefined, 'bold');
            doc.text('Water Allocation by Area', this.margin, yPos);
            yPos += 5;
            
            const tableData = reportData.allocationByArea.map(area => [
                area.areaName || 'Unknown',
                this.formatNumber(area.totalAbstraction),
                this.formatNumber(area.allocationLevel) + '%',
                area.status || 'Unknown'
            ]);
            
            doc.autoTable({
                startY: yPos,
                head: [['Area Name', 'Abstraction (ML/year)', 'Allocation Level', 'Status']],
                body: tableData,
                margin: { left: this.margin, right: this.margin },
                styles: { fontSize: 9 },
                columnStyles: {
                    0: { cellWidth: 50 },
                    1: { cellWidth: 40, halign: 'right' },
                    2: { cellWidth: 35, halign: 'right' },
                    3: { cellWidth: 35, halign: 'center' }
                }
            });
        }
        
        this.addHeaderFooter(doc, title);
        doc.save('water-allocation-level-report.pdf');
    }

    // 2. Water Discharge Distribution Report
    generateWaterDischargeDistributionReport(reportData) {
        const doc = new this.jsPDF();
        const title = 'Water Discharge Distribution Report';
        
        this.generateDistributionReport(doc, reportData, title, 'Discharge Volume');
        this.addHeaderFooter(doc, title);
        doc.save('water-discharge-distribution-report.pdf');
    }

    // 3. Water Use Distribution Report
    generateWaterUseDistributionReport(reportData) {
        const doc = new this.jsPDF();
        const title = 'Water Use Distribution Report';
        
        this.generateDistributionReport(doc, reportData, title, 'Usage Volume');
        this.addHeaderFooter(doc, title);
        doc.save('water-use-distribution-report.pdf');
    }

    // 4. Water Permits Distribution Report
    generateWaterPermitsDistributionReport(reportData) {
        const doc = new this.jsPDF();
        const title = 'Water Permits Distribution Report';
        
        this.generateDistributionReport(doc, reportData, title, 'Number of Permits');
        this.addHeaderFooter(doc, title);
        doc.save('water-permits-distribution-report.pdf');
    }

    // 5. Water License Debt Distribution Report
    generateWaterLicenseDebtDistributionReport(reportData) {
        const doc = new this.jsPDF();
        const title = 'Water License Debt Distribution Report';
        
        this.generateDistributionReport(doc, reportData, title, 'Outstanding Debt', true);
        this.addHeaderFooter(doc, title);
        doc.save('water-license-debt-distribution-report.pdf');
    }

    // 6. Water License Revenue Distribution Report
    generateWaterLicenseRevenueDistributionReport(reportData) {
        const doc = new this.jsPDF();
        const title = 'Water License Revenue Distribution Report';
        
        this.generateDistributionReport(doc, reportData, title, 'Total Revenue', true);
        this.addHeaderFooter(doc, title);
        doc.save('water-license-revenue-distribution-report.pdf');
    }

    // 7. Largest Water Users Report
    generateLargestWaterUsersReport(reportData) {
        const doc = new this.jsPDF();
        const title = 'Largest Water Users Report';
        
        let yPos = 40;
        
        if (reportData.largestUsers && reportData.largestUsers.length > 0) {
            const tableData = reportData.largestUsers.map((user, index) => [
                (index + 1).toString(),
                user.licenseHolder || 'Unknown',
                user.licenseType || 'Unknown',
                this.formatNumber(user.totalAbstraction),
                user.areaName || 'Unknown'
            ]);
            
            doc.autoTable({
                startY: yPos,
                head: [['Rank', 'License Holder', 'License Type', 'Abstraction (ML/year)', 'Area']],
                body: tableData,
                margin: { left: this.margin, right: this.margin },
                styles: { fontSize: 9 },
                columnStyles: {
                    0: { cellWidth: 15, halign: 'center' },
                    1: { cellWidth: 50 },
                    2: { cellWidth: 35 },
                    3: { cellWidth: 40, halign: 'right' },
                    4: { cellWidth: 40 }
                }
            });
        }
        
        this.addHeaderFooter(doc, title);
        doc.save('largest-water-users-report.pdf');
    }

    // 8. Largest Water Discharge Report
    generateLargestDischargeReport(reportData) {
        const doc = new this.jsPDF();
        const title = 'Largest Water Discharge Report';
        
        let yPos = 40;
        
        if (reportData.largestDischarge && reportData.largestDischarge.length > 0) {
            const tableData = reportData.largestDischarge.map((item, index) => [
                (index + 1).toString(),
                item.licenseHolder || 'Unknown',
                item.licenseType || 'Unknown',
                this.formatNumber(item.dischargeVolume),
                item.areaName || 'Unknown'
            ]);
            
            doc.autoTable({
                startY: yPos,
                head: [['Rank', 'License Holder', 'License Type', 'Discharge Volume', 'Area']],
                body: tableData,
                margin: { left: this.margin, right: this.margin },
                styles: { fontSize: 9 },
                columnStyles: {
                    0: { cellWidth: 15, halign: 'center' },
                    1: { cellWidth: 50 },
                    2: { cellWidth: 35 },
                    3: { cellWidth: 40, halign: 'right' },
                    4: { cellWidth: 40 }
                }
            });
        }
        
        this.addHeaderFooter(doc, title);
        doc.save('largest-water-discharge-report.pdf');
    }

    // 9. Largest Debt Holders Report
    generateLargestDebtHoldersReport(reportData) {
        const doc = new this.jsPDF();
        const title = 'Largest Debt Holders Report';
        
        let yPos = 40;
        
        if (reportData.largestDebtHolders && reportData.largestDebtHolders.length > 0) {
            const tableData = reportData.largestDebtHolders.map((holder, index) => [
                (index + 1).toString(),
                holder.licenseHolder || 'Unknown',
                holder.licenseType || 'Unknown',
                this.formatCurrency(holder.outstandingDebt),
                holder.areaName || 'Unknown'
            ]);
            
            doc.autoTable({
                startY: yPos,
                head: [['Rank', 'License Holder', 'License Type', 'Outstanding Debt', 'Area']],
                body: tableData,
                margin: { left: this.margin, right: this.margin },
                styles: { fontSize: 9 },
                columnStyles: {
                    0: { cellWidth: 15, halign: 'center' },
                    1: { cellWidth: 50 },
                    2: { cellWidth: 35 },
                    3: { cellWidth: 40, halign: 'right' },
                    4: { cellWidth: 40 }
                }
            });
        }
        
        this.addHeaderFooter(doc, title);
        doc.save('largest-debt-holders-report.pdf');
    }

    // 10. Largest Revenue Licences Report
    generateLargestRevenueLicencesReport(reportData) {
        const doc = new this.jsPDF();
        const title = 'Largest Revenue Licences Report';
        
        let yPos = 40;
        
        if (reportData.largestRevenueLicences && reportData.largestRevenueLicences.length > 0) {
            const tableData = reportData.largestRevenueLicences.map((revenue, index) => [
                (index + 1).toString(),
                revenue.licenseHolder || 'Unknown',
                revenue.licenseType || 'Unknown',
                this.formatCurrency(revenue.totalRevenue),
                revenue.areaName || 'Unknown'
            ]);
            
            doc.autoTable({
                startY: yPos,
                head: [['Rank', 'License Holder', 'License Type', 'Total Revenue', 'Area']],
                body: tableData,
                margin: { left: this.margin, right: this.margin },
                styles: { fontSize: 9 },
                columnStyles: {
                    0: { cellWidth: 15, halign: 'center' },
                    1: { cellWidth: 50 },
                    2: { cellWidth: 35 },
                    3: { cellWidth: 40, halign: 'right' },
                    4: { cellWidth: 40 }
                }
            });
        }
        
        this.addHeaderFooter(doc, title);
        doc.save('largest-revenue-licences-report.pdf');
    }

    // Helper method for distribution reports
    generateDistributionReport(doc, reportData, title, valueColumnName, isCurrency = false) {
        let yPos = 40;
        
        // Summary section
        if (reportData.summary) {
            doc.setFontSize(12);
            doc.setFont(undefined, 'bold');
            doc.text('Summary', this.margin, yPos);
            yPos += 10;
            
            const summaryData = Object.entries(reportData.summary).map(([key, value]) => [
                this.formatKey(key),
                isCurrency ? this.formatCurrency(value) : this.formatNumber(value)
            ]);
            
            doc.autoTable({
                startY: yPos,
                head: [['Metric', 'Value']],
                body: summaryData,
                margin: { left: this.margin, right: this.margin },
                styles: { fontSize: 10 }
            });
            
            yPos = doc.lastAutoTable.finalY + 15;
        }
        
        // Distribution data
        if (reportData.distribution && reportData.distribution.length > 0) {
            doc.setFontSize(12);
            doc.setFont(undefined, 'bold');
            doc.text('Distribution by Area', this.margin, yPos);
            yPos += 5;
            
            const tableData = reportData.distribution.map(item => [
                item.areaName || 'Unknown',
                item.count || 0,
                isCurrency ? this.formatCurrency(item.totalValue) : this.formatNumber(item.totalValue),
                this.formatNumber(item.percentage) + '%'
            ]);
            
            doc.autoTable({
                startY: yPos,
                head: [['Area Name', 'Count', valueColumnName, 'Percentage']],
                body: tableData,
                margin: { left: this.margin, right: this.margin },
                styles: { fontSize: 9 },
                columnStyles: {
                    0: { cellWidth: 60 },
                    1: { cellWidth: 30, halign: 'right' },
                    2: { cellWidth: 50, halign: 'right' },
                    3: { cellWidth: 30, halign: 'right' }
                }
            });
        }
    }

    // Helper method to format keys
    formatKey(key) {
        return key.replace(/([a-z])([A-Z])/g, '$1 $2')
                  .replace(/_/g, ' ')
                  .replace(/\b\w/g, l => l.toUpperCase());
    }
}

// Usage examples and integration code
const pdfGenerator = new WaterReportPDFGenerator();

// Function to add download buttons to your reports
function addDownloadButtons() {
    const reportConfigs = [
        { id: 'water-allocation-level', method: 'generateWaterAllocationLevelReport', title: 'Water Allocation Level' },
        { id: 'water-discharge-distribution', method: 'generateWaterDischargeDistributionReport', title: 'Water Discharge Distribution' },
        { id: 'water-use-distribution', method: 'generateWaterUseDistributionReport', title: 'Water Use Distribution' },
        { id: 'water-permits-distribution', method: 'generateWaterPermitsDistributionReport', title: 'Water Permits Distribution' },
        { id: 'water-license-debt-distribution', method: 'generateWaterLicenseDebtDistributionReport', title: 'Water License Debt Distribution' },
        { id: 'water-license-revenue-distribution', method: 'generateWaterLicenseRevenueDistributionReport', title: 'Water License Revenue Distribution' },
        { id: 'largest-water-users', method: 'generateLargestWaterUsersReport', title: 'Largest Water Users' },
        { id: 'largest-water-discharge', method: 'generateLargestDischargeReport', title: 'Largest Water Discharge' },
        { id: 'largest-debt-holders', method: 'generateLargestDebtHoldersReport', title: 'Largest Debt Holders' },
        { id: 'largest-revenue-licences', method: 'generateLargestRevenueLicencesReport', title: 'Largest Revenue Licences' }
    ];

    reportConfigs.forEach(config => {
        const reportContainer = document.getElementById(config.id);
        if (reportContainer) {
            // Create download button
            const downloadBtn = document.createElement('button');
            downloadBtn.className = 'btn btn-primary download-pdf-btn';
            downloadBtn.innerHTML = '<i class="fas fa-download"></i> Download PDF';
            downloadBtn.style.marginLeft = '10px';
            
            // Add click event
            downloadBtn.addEventListener('click', function() {
                // Get the report data from your global variables or data store
                const reportData = getReportData(config.id); // You need to implement this
                if (reportData && pdfGenerator[config.method]) {
                    pdfGenerator[config.method](reportData);
                } else {
                    alert('Report data not available. Please load the report first.');
                }
            });
            
            // Find the best place to insert the button (usually in a header or toolbar)
            const header = reportContainer.querySelector('.card-header, .report-header, h3, h4');
            if (header) {
                header.appendChild(downloadBtn);
            } else {
                reportContainer.insertBefore(downloadBtn, reportContainer.firstChild);
            }
        }
    });
}

// You need to implement this function to get report data from your application
function getReportData(reportId) {
    // This should return the same data structure that your API returns
    // You can store this in global variables when you load the reports
    switch(reportId) {
        case 'water-allocation-level':
            return window.waterAllocationLevelData || null;
        case 'water-discharge-distribution':
            return window.waterDischargeDistributionData || null;
        case 'water-use-distribution':
            return window.waterUseDistributionData || null;
        case 'water-permits-distribution':
            return window.waterPermitsDistributionData || null;
        case 'water-license-debt-distribution':
            return window.waterLicenseDebtDistributionData || null;
        case 'water-license-revenue-distribution':
            return window.waterLicenseRevenueDistributionData || null;
        case 'largest-water-users':
            return window.largestWaterUsersData || null;
        case 'largest-water-discharge':
            return window.largestWaterDischargeData || null;
        case 'largest-debt-holders':
            return window.largestDebtHoldersData || null;
        case 'largest-revenue-licences':
            return window.largestRevenueLicencesData || null;
        default:
            return null;
    }
}

// Call this function after your reports are loaded
document.addEventListener('DOMContentLoaded', function() {
    // Add download buttons when the page loads
    setTimeout(addDownloadButtons, 1000); // Delay to ensure reports are loaded
});

/* 
INTEGRATION INSTRUCTIONS:

1. Include the required libraries in your HTML:
   <script src="https://cdnjs.cloudflare.com/ajax/libs/jspdf/2.5.1/jspdf.umd.min.js"></script>
   <script src="https://cdnjs.cloudflare.com/ajax/libs/jspdf-autotable/3.5.25/jspdf.plugin.autotable.min.js"></script>

2. Include this script in your HTML:
   <script src="frontend-pdf-utils.js"></script>

3. Store report data in global variables when you load them:
   window.waterAllocationLevelData = responseData;
   window.waterDischargeDistributionData = responseData;
   // ... etc for all reports

4. The download buttons will be automatically added to your report containers.

5. Make sure your report containers have the correct IDs as defined in reportConfigs.

6. Customize the button styling and placement as needed for your UI.
*/