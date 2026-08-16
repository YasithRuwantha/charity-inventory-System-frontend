package com.charitymanagement.api.charitymanagementback.reporting.service;

import com.charitymanagement.api.charitymanagementback.common.exception.BadRequestException;
import com.charitymanagement.api.charitymanagementback.inventory.dto.response.InventoryResponse;
import com.charitymanagement.api.charitymanagementback.inventory.dto.response.InventoryTransactionResponse;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.BeneficiaryReportResponse;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.DistributionReportSummaryResponse;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.DonationReportResponse;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.InventoryReportResponse;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.ReportBreakdownEntry;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.StockMovementReportResponse;
import com.charitymanagement.api.charitymanagementback.reporting.dto.response.VolunteerReportResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

/**
 * Renders report DTOs as CSV.
 *
 * <p>Uses commons-csv rather than string concatenation so values containing commas, quotes or
 * newlines are escaped correctly instead of corrupting the file.
 */
@Service
public class CsvExportService {

    public byte[] inventoryReport(InventoryReportResponse report) {
        return write(printer -> {
            printer.printRecord("Metric", "Value");
            printer.printRecord("Total items", report.getTotalItems());
            printer.printRecord("Total quantity", report.getTotalQuantity());
            printer.printRecord("Low stock items", report.getLowStockItems());
            printer.printRecord("Out of stock items", report.getOutOfStockItems());
            printer.printRecord("Expired items", report.getExpiredItems());
            printer.printRecord("Expiring soon items", report.getExpiringSoonItems());
            printer.println();
            printer.printRecord("Category", "Items", "Quantity");
            for (ReportBreakdownEntry entry : report.getCategoryBreakdown()) {
                printer.printRecord(entry.getLabel(), entry.getCount(), entry.getQuantity());
            }
            printer.println();
            printer.printRecord("Status", "Items");
            for (ReportBreakdownEntry entry : report.getStatusBreakdown()) {
                printer.printRecord(entry.getLabel(), entry.getCount());
            }
        });
    }

    public byte[] donationReport(DonationReportResponse report) {
        return write(printer -> {
            printer.printRecord("Metric", "Value");
            printer.printRecord("Total donations", report.getTotalDonations());
            printer.printRecord("Total item lines", report.getTotalItemLines());
            printer.printRecord("Total quantity donated", report.getTotalQuantityDonated());
            printer.println();
            printer.printRecord("Donor code", "Donor", "Donations", "Quantity");
            for (ReportBreakdownEntry entry : report.getDonorContributions()) {
                printer.printRecord(entry.getCode(), entry.getLabel(), entry.getCount(), entry.getQuantity());
            }
        });
    }

    public byte[] distributionReport(DistributionReportSummaryResponse report) {
        return write(printer -> {
            printer.printRecord("Metric", "Value");
            printer.printRecord("Completed distributions", report.getCompletedDistributions());
            printer.printRecord("Total item lines", report.getTotalItemLines());
            printer.printRecord("Total quantity distributed", report.getTotalQuantityDistributed());
            printer.println();
            printer.printRecord("Status", "Requests");
            for (ReportBreakdownEntry entry : report.getStatusBreakdown()) {
                printer.printRecord(entry.getLabel(), entry.getCount());
            }
            printer.println();
            printer.printRecord("Priority", "Requests");
            for (ReportBreakdownEntry entry : report.getPriorityBreakdown()) {
                printer.printRecord(entry.getLabel(), entry.getCount());
            }
            printer.println();
            printer.printRecord("Beneficiary code", "Beneficiary", "Distributions", "Quantity received");
            for (ReportBreakdownEntry entry : report.getBeneficiaryBreakdown()) {
                printer.printRecord(entry.getCode(), entry.getLabel(), entry.getCount(), entry.getQuantity());
            }
            printer.println();
            printer.printRecord("Item code", "Item", "Quantity distributed");
            for (ReportBreakdownEntry entry : report.getItemBreakdown()) {
                printer.printRecord(entry.getCode(), entry.getLabel(), entry.getQuantity());
            }
        });
    }

    public byte[] beneficiaryReport(BeneficiaryReportResponse report) {
        return write(printer -> {
            printer.printRecord("Metric", "Value");
            printer.printRecord("Total beneficiaries", report.getTotalBeneficiaries());
            printer.printRecord("Active", report.getActiveBeneficiaries());
            printer.printRecord("Inactive", report.getInactiveBeneficiaries());
            printer.println();
            printer.printRecord("Priority", "Beneficiaries");
            for (ReportBreakdownEntry entry : report.getPriorityBreakdown()) {
                printer.printRecord(entry.getLabel(), entry.getCount());
            }
            printer.println();
            printer.printRecord("Beneficiary code", "Beneficiary", "Distributions", "Quantity received");
            for (ReportBreakdownEntry entry : report.getDistributionBreakdown()) {
                printer.printRecord(entry.getCode(), entry.getLabel(), entry.getCount(), entry.getQuantity());
            }
        });
    }

    public byte[] volunteerReport(VolunteerReportResponse report) {
        return write(printer -> {
            printer.printRecord("Metric", "Value");
            printer.printRecord("Total volunteers", report.getTotalVolunteers());
            printer.printRecord("Active", report.getActiveVolunteers());
            printer.printRecord("Inactive", report.getInactiveVolunteers());
            printer.printRecord("Total tasks", report.getTotalTasks());
            printer.printRecord("Pending tasks", report.getPendingTasks());
            printer.printRecord("In progress tasks", report.getInProgressTasks());
            printer.printRecord("Completed tasks", report.getCompletedTasks());
            printer.printRecord("Cancelled tasks", report.getCancelledTasks());
            printer.printRecord("Overdue tasks", report.getOverdueTasks());
            printer.println();
            printer.printRecord("Volunteer code", "Volunteer", "Total tasks", "Completed tasks");
            for (ReportBreakdownEntry entry : report.getVolunteerBreakdown()) {
                printer.printRecord(entry.getCode(), entry.getLabel(), entry.getCount(), entry.getQuantity());
            }
        });
    }

    public byte[] stockMovementReport(StockMovementReportResponse report) {
        return write(printer -> {
            printer.printRecord("Metric", "Value");
            printer.printRecord("Total movements", report.getTotalMovements());
            printer.printRecord("Quantity in", report.getTotalQuantityIn());
            printer.printRecord("Quantity out", report.getTotalQuantityOut());
            printer.println();
            printer.printRecord("Date", "Item code", "Item", "Type", "Quantity", "Before", "After",
                    "Reference type", "Reference id", "Performed by", "Notes");
            for (InventoryTransactionResponse movement : report.getMovements()) {
                printer.printRecord(
                        movement.getCreatedAt(),
                        movement.getItemCode(),
                        movement.getItemName(),
                        movement.getTransactionType(),
                        movement.getQuantity(),
                        movement.getQuantityBefore(),
                        movement.getQuantityAfter(),
                        movement.getReferenceType(),
                        movement.getReferenceId(),
                        movement.getPerformedBy() != null ? movement.getPerformedBy().getEmail() : null,
                        movement.getNotes());
            }
        });
    }

    public byte[] expiryReport(List<InventoryResponse> items) {
        return write(printer -> {
            printer.printRecord("Item code", "Item", "Category", "Quantity", "Unit", "Expiry date",
                    "Days until expiry", "Status");
            for (InventoryResponse item : items) {
                printer.printRecord(item.getItemCode(), item.getItemName(), item.getCategoryName(),
                        item.getQuantity(), item.getUnit(), item.getExpiryDate(), item.getDaysUntilExpiry(),
                        item.getStatus());
            }
        });
    }

    private byte[] write(CsvBody body) {
        StringWriter writer = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            body.accept(printer);
            printer.flush();
        } catch (IOException ex) {
            throw new BadRequestException("Could not generate the CSV export: " + ex.getMessage(),
                    "EXPORT_FAILED");
        }
        return writer.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** {@link Consumer} that may throw, so the printing lambdas stay readable. */
    @FunctionalInterface
    private interface CsvBody {
        void accept(CSVPrinter printer) throws IOException;
    }
}
