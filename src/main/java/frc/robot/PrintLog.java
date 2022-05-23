// Temporarily added for testing, copied from WPILib Github

package frc.robot;

import edu.wpi.first.util.datalog.DataLogReader;
import edu.wpi.first.util.datalog.DataLogRecord;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Map;

public class PrintLog {

    private static final DateTimeFormatter m_timeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void printLog(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: printlog <file>");
            System.exit(1);
            return;
        }
        DataLogReader reader;
        try {
            reader = new DataLogReader(args[0]);
        } catch (IOException ex) {
            System.err.println("could not open file: " + ex.getMessage());
            System.exit(1);
            return;
        }
        if (!reader.isValid()) {
            System.err.println("not a log file");
            System.exit(1);
            return;
        }

        Map<Integer, DataLogRecord.StartRecordData> entries = new HashMap<>();
        for (DataLogRecord record : reader) {
            if (record.isStart()) {
                try {
                    DataLogRecord.StartRecordData data = record.getStartData();
                    System.out.println(
                            "Start("
                                    + data.entry
                                    + ", name='"
                                    + data.name
                                    + "', type='"
                                    + data.type
                                    + "', metadata='"
                                    + data.metadata
                                    + "') ["
                                    + (record.getTimestamp() / 1000000.0)
                                    + "]");
                    if (entries.containsKey(data.entry)) {
                        System.out.println("...DUPLICATE entry ID, overriding");
                    }
                    entries.put(data.entry, data);
                } catch (InputMismatchException ex) {
                    System.out.println("Start(INVALID)");
                }
            } else if (record.isFinish()) {
                try {
                    int entry = record.getFinishEntry();
                    System.out.println("Finish(" + entry + ") [" + (record.getTimestamp() / 1000000.0) + "]");
                    if (!entries.containsKey(entry)) {
                        System.out.println("...ID not found");
                    } else {
                        entries.remove(entry);
                    }
                } catch (InputMismatchException ex) {
                    System.out.println("Finish(INVALID)");
                }
            } else if (record.isSetMetadata()) {
                try {
                    DataLogRecord.MetadataRecordData data = record.getSetMetadataData();
                    System.out.println(
                            "SetMetadata("
                                    + data.entry
                                    + ", '"
                                    + data.metadata
                                    + "') ["
                                    + (record.getTimestamp() / 1000000.0)
                                    + "]");
                    if (!entries.containsKey(data.entry)) {
                        System.out.println("...ID not found");
                    }
                } catch (InputMismatchException ex) {
                    System.out.println("SetMetadata(INVALID)");
                }
            } else if (record.isControl()) {
                System.out.println("Unrecognized control record");
            } else {
                System.out.print("Data(" + record.getEntry() + ", size=" + record.getSize() + ") ");
                DataLogRecord.StartRecordData entry = entries.get(record.getEntry());
                if (entry == null) {
                    System.out.println("<ID not found>");
                    continue;
                }
                System.out.println(
                        "<name='"
                                + entry.name
                                + "', type='"
                                + entry.type
                                + "'> ["
                                + (record.getTimestamp() / 1000000.0)
                                + "]");

                try {
                    // handle systemTime specially
                    if ("systemTime".equals(entry.name) && "int64".equals(entry.type)) {
                        long val = record.getInteger();
                        System.out.println(
                                "  "
                                        + m_timeFormatter.format(
                                        LocalDateTime.ofEpochSecond(val / 1000000, 0, ZoneOffset.UTC))
                                        + "."
                                        + String.format("%06d", val % 1000000));
                        continue;
                    }

                    if ("double".equals(entry.type)) {
                        System.out.println("  " + record.getDouble());
                    } else if ("int64".equals(entry.type)) {
                        System.out.println("  " + record.getInteger());
                    } else if ("string".equals(entry.type) || "json".equals(entry.type)) {
                        System.out.println("  '" + record.getString() + "'");
                    } else if ("boolean".equals(entry.type)) {
                        System.out.println("  " + record.getBoolean());
                    } else if ("double[]".equals(entry.type)) {
                        System.out.println("  " + Arrays.asList(record.getDoubleArray()));
                    } else if ("int64[]".equals(entry.type)) {
                        System.out.println("  " + Arrays.asList(record.getIntegerArray()));
                    } else if ("string[]".equals(entry.type)) {
                        System.out.println("  " + Arrays.asList(record.getStringArray()));
                    }
                } catch (InputMismatchException ex) {
                    System.out.println("  invalid");
                }
            }
        }
    }
}
