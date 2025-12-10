package org.example.se302.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of a CSV import operation.
 */
public class ImportResult {
    private boolean success;
    private int recordCount;
    private List<String> errors;
    private String message;

    public ImportResult() {
        this.success = false;
        this.recordCount = 0;
        this.errors = new ArrayList<>();
        this.message = "";
    }

    public ImportResult(boolean success, int recordCount, String message) {
        this.success = success;
        this.recordCount = recordCount;
        this.message = message;
        this.errors = new ArrayList<>();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(int recordCount) {
        this.recordCount = recordCount;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void addError(String error) {
        this.errors.add(error);
        this.success = false;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public String getFormattedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(message).append("\n");

        if (hasErrors()) {
            sb.append("\nErrors:\n");
            for (String error : errors) {
                sb.append("  - ").append(error).append("\n");
            }
        }

        return sb.toString();
    }
}
