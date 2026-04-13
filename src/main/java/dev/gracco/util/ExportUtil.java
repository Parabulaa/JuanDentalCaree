package dev.gracco.util;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class ExportUtil {

    public static void exportToCSV(JTable table, String defaultFileName, JFrame parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(defaultFileName + ".csv"));
        chooser.setDialogTitle("Save CSV");

        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().endsWith(".csv")) {
            file = new File(file.getAbsolutePath() + ".csv");
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            // Header
            StringBuilder header = new StringBuilder();
            for (int i = 0; i < table.getColumnCount(); i++) {
                if (i > 0) header.append(",");
                header.append(escape(table.getColumnName(i)));
            }
            pw.println(header);

            // Rows
            for (int r = 0; r < table.getRowCount(); r++) {
                StringBuilder row = new StringBuilder();
                for (int c = 0; c < table.getColumnCount(); c++) {
                    if (c > 0) row.append(",");
                    Object val = table.getValueAt(r, c);
                    row.append(escape(val == null ? "" : val.toString()));
                }
                pw.println(row);
            }

            JOptionPane.showMessageDialog(parent, "Exported to " + file.getName(), "Export Successful",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parent, "Export failed: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String escape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
