package os.process;

import os.Main;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ProcessController {
    public static PCB createProcess(int PID, int priority, String filename) {
        List<String[]> instructions = readScript(filename);
        return new PCB(PID, priority, instructions.size(), instructions);
    }

    // 读取脚本文件
    private static List<String[]> readScript(String filename) {
        try (InputStream stream = Main.class.getClassLoader().getResourceAsStream(filename)) {
            String s = new String(stream.readAllBytes(), Charset.defaultCharset());
            return Arrays.stream(s.split("\r\n")).map(str -> str.split(",")).collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Error reading script file: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
