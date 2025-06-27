package org.diplom.keylogger;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Autostart {
    private static final Logger logger = Logger.getLogger(Autostart.class.getName());

    public static void setAutostart() {
        try {
            Path exePath = Paths.get(Main.class.getProtectionDomain()
                            .getCodeSource().getLocation().toURI())
                    .toAbsolutePath();

            if (!exePath.toString().endsWith(".exe")) {
                exePath = exePath.resolve("..")
                        .resolve("PushToTalkUtility.exe")
                        .normalize();
            }
            final Path finalExePath = exePath;
            logger.fine("exe path: " + finalExePath);
            InputStream regStream = Main.class.getResourceAsStream("/Autostart.reg");
            if (regStream == null) {
                throw new IOException("Autostart not found");
            }
            List<String> lines = new BufferedReader(new InputStreamReader(regStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.toList());
            List<String> updated = lines.stream()
                    .map(line -> line.replace("%EXE_PATH%", finalExePath.toString().replace("\\", "\\\\")))
                    .collect(Collectors.toList());
            Path tempReg = Files.createTempFile("autostart", ".reg");
            logger.fine("temp .reg file path: " + tempReg);
            try (OutputStream out = Files.newOutputStream(tempReg);
                 OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_16LE)) {
                out.write(0xFF);
                out.write(0xFE);
                for (String line : updated) {
                    writer.write(line);
                    writer.write("\r\n");
                }
            }

            boolean success=runRegeditWithUAC(tempReg);
            if (!success) {
                logger.warning("autostart setting declined by user");
            }else if(isAutostartRegistered()){
                logger.info("autostart was set successfully");
            }else{
                logger.warning("autostart was not set because of unknown error");
            }
            Files.deleteIfExists(tempReg);
        }catch (URISyntaxException | IOException | InterruptedException e){
            logger.warning("autostart setting failed: "+e.getMessage());
            logger.fine(Arrays.toString(e.getStackTrace()));
        }
    }

    public static boolean runRegeditWithUAC(Path regFile) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("powershell", "-Command",
                "Start-Process", "regedit",
                "-ArgumentList", "'/s \"" + regFile.toAbsolutePath().toString().replace("\"", "\\\"") + "\"'",
                "-Verb", "runAs"
        );
        pb.inheritIO();
        Process process=pb.start();
        int exitCode=process.waitFor();
        logger.fine("autostart settings process exited with exit code: "+exitCode);
        return exitCode==0;
    }

    public static boolean isAutostartRegistered() {
        try {
            Process p = Runtime.getRuntime().exec("reg query HKEY_CLASSES_ROOT\\pttutility");
            int exitCode=p.waitFor();
            logger.fine("autostart check process exited with exit code: "+exitCode);
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static void removeAutostart() {
        if(!isAutostartRegistered()) {
            logger.warning("autostart isn't set, skip removing");
            return;
        }
        logger.info("removing autostart...");
        try {
            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command",
                    "Start-Process", "cmd",
                    "-ArgumentList", "\"/c reg delete HKEY_CLASSES_ROOT\\pttutility /f\"",
                    "-Verb", "runAs");
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logger.info("registry entry removed successfully");
            } else {
                logger.warning("an error occurred when removing registry entry");
                logger.fine("removing process exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            logger.warning("start removing registry entry process error: " + e.getMessage() );
            logger.fine(Arrays.toString(e.getStackTrace()));
        }
    }

    public static Logger getLogger() {
        return logger;
    }
}
