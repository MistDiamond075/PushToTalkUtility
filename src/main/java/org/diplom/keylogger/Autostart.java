package org.diplom.keylogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
                    .resolve("..")
                    .resolve("PushToTalkUtility.exe")
                    .normalize();
            logger.fine("exe path: " + exePath);
            InputStream regStream = Main.class.getResourceAsStream("/Autostart.reg");
            if (regStream == null) {
                throw new IOException("Autostart not found");
            }
            List<String> lines = new BufferedReader(new InputStreamReader(regStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.toList());
            List<String> updated = lines.stream()
                    .map(line -> line.replace("%EXE_PATH%", exePath.toString().replace("\\", "\\\\")))
                    .collect(Collectors.toList());
            Path tempReg = Files.createTempFile("autostart", ".reg");
            logger.fine("temp .reg file path: " + tempReg);
            Files.write(tempReg, updated);

            boolean success=runRegeditWithUAC(tempReg);
            if (!success) {
                logger.warning("autostart setting declined by user");
            }else {
                logger.info("autostart was set successfully");
            }
        }catch (URISyntaxException | IOException | InterruptedException e){
            logger.warning("autostart setting failed: "+e.getMessage());
            logger.fine(Arrays.toString(e.getStackTrace()));
        }
    }

    public static boolean runRegeditWithUAC(Path regFile) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("powershell", "-Command",
                "Start-Process", "\"cmd\"",
                "-ArgumentList", "'/c regedit /s \"" + regFile.toAbsolutePath() + "\"'",
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

    public static Logger getLogger() {
        return logger;
    }
}
