package os;

import os.filesystem.FileSystem;
import os.process.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

import static os.process.ProcessController.createProcess;
public class Main {

    public static boolean debug = false;

    public static void main(String[] args) {
        Simulator simulator = bootstrap();

        simulator.simulate(
            List.of(
                createProcess(0, 0, "write_file.txt"),
                createProcess(1, 1, "script.txt")
        ));

    }

    private static Simulator bootstrap() {
        System.out.println("Bootstrapping...");
        Scanner scanner = new Scanner(System.in);
        System.out.println("debug?[y/N]");
        String debugInput = scanner.nextLine();
        if (debugInput.equalsIgnoreCase("y")) {
            debug = true;
        }
        System.out.println("debug mode: " + debug);
        System.out.println("choose a schedule algorithm:1-RR, 2-MLFQ, 3-Priority");
        int algorithm = scanner.nextInt();
        Scheduler scheduler;
        switch (algorithm) {
            case 1:
                scheduler = new RoundRobinScheduler(2);
                break;
            case 2:
                scheduler = new MLFQScheduler(
                    List.of(
                            new MLFQScheduler.QueueConfig(0, 1, true),
                            new MLFQScheduler.QueueConfig(1, 2, true),
                            new MLFQScheduler.QueueConfig(2, Integer.MAX_VALUE, false)
                    )
                );
                break;
            case 3:
                scheduler = new PriorityScheduler();
                break;
            default:
                System.out.println("Invalid choice. Using default rr");
                scheduler = new RoundRobinScheduler(2);
                break;
        }

        System.out.println("Deleting old file system structure...");
        String fileRoot = System.getProperty("user.dir") + File.separator + "virtualfs";
        try {
            deleteFile(Paths.get(fileRoot));
        } catch (IOException e) {
            System.out.println("Failed to delete file system root: " + e.getMessage());
        }
        System.out.println("Deleting old file system success!");

        System.out.println("Bootstrapping complete.");
        return new Simulator(scheduler);
    }

    private static void deleteFile(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walkFileTree(path, new FileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
