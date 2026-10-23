
package compTest3011;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import comp3011.FrameBlackAndWhiter;
import comp3011.FrameBleeder;
import comp3011.InfoFrame;
import comp3011.InfoVideo;
import org.bytedeco.javacv.Frame;
import java.nio.ByteBuffer;

public class MultithreadTest {

    @Test
    void testMultipleThreadsProcessTasks() throws Exception {

        int taskCount = 100;
        int threadCount = 4;

        ExecutorService executor =
                Executors.newFixedThreadPool(threadCount);
        
        try {
            List<Callable<Integer>> tasks = new ArrayList<>();

            for (int i = 0; i < taskCount; i++) {
                final int frameNumber = i;

                tasks.add(() -> {
                    return frameNumber * 2;
                });
            }

            List<Future<Integer>> results =
                    executor.invokeAll(tasks);

            assertEquals(taskCount, results.size());

            for (int i = 0; i < taskCount; i++) {
                assertEquals(i * 2, results.get(i).get());
            }

        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(
                    5, TimeUnit.SECONDS));
        }
    }
@Test
void testConcurrentFrameProcessingMatchesSequential() throws Exception {

	int width = 1280;
	int height = 720;
	int channels = 3;
	int frameCount = 30;
    FrameBlackAndWhiter processor = new FrameBlackAndWhiter();

    InfoVideo videoInfo = new InfoVideo(
            "test",
            frameCount,
            width,
            height,
            Frame.DEPTH_UBYTE,
            channels,
            width * channels,
            24.0,
            24,
            41667,
            0
    );

    processor.initialise(videoInfo);

    // Create identical frames for sequential and concurrent processing.
    List<Frame> sequentialFrames = new ArrayList<>();
    List<Frame> concurrentFrames = new ArrayList<>();

    for (int i = 0; i < frameCount; i++) {
        Frame frame = new Frame(width, height, Frame.DEPTH_UBYTE, channels);
        ByteBuffer pixels = (ByteBuffer) frame.image[0];

        for (int j = 0; j < width * height * channels; j++) {
            pixels.put(j, (byte) ((i + j) % 256));
        }

        sequentialFrames.add(frame);
        concurrentFrames.add(frame.clone());
    }
    // JVM before measuring performance.
    for (int i = 0; i < 5; i++) {
        Frame warmupFrame = sequentialFrames.get(i).clone();

        processor.process(
                warmupFrame,
                new InfoFrame(i, i * 41667L)
        );
    }
    long sequentialStart = System.nanoTime();
    // Process frames sequentially.
    for (int i = 0; i < frameCount; i++) {
        processor.process(
                sequentialFrames.get(i),
                new InfoFrame(i, i * 41667L)
        );
    }
    
    long sequentialTime = System.nanoTime() - sequentialStart;
    // Process frames using multiple worker threads.
    ExecutorService executor = Executors.newFixedThreadPool(4);

    try {
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < frameCount; i++) {
            final int frameNumber = i;

            tasks.add(() -> {
                processor.process(
                        concurrentFrames.get(frameNumber),
                        new InfoFrame(frameNumber, frameNumber * 41667L)
                );
                return null;
            });
        }
        long concurrentStart = System.nanoTime();

        for (Future<Void> result : executor.invokeAll(tasks)) {
            result.get();
        }

        long concurrentTime = System.nanoTime() - concurrentStart;

        System.out.println("Sequential time: "
                + sequentialTime / 1_000_000.0 + " ms");

        System.out.println("Concurrent time: "
                + concurrentTime / 1_000_000.0 + " ms");
        // Compare every pixel in every frame.
        for (int i = 0; i < frameCount; i++) {
            ByteBuffer expected =
                    (ByteBuffer) sequentialFrames.get(i).image[0];

            ByteBuffer actual =
                    (ByteBuffer) concurrentFrames.get(i).image[0];

            assertEquals(expected.capacity(), actual.capacity());

            for (int j = 0; j < expected.capacity(); j++) {
                assertEquals(
                        expected.get(j),
                        actual.get(j),
                        "Pixel mismatch in frame " + i + " at byte " + j
                );
            }
        }

    } finally {
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
}

@Test
void testFrameBleederPerformance() throws Exception {

    int width = 1280;
    int height = 720;
    int channels = 3;
    int frameCount = 12;

    FrameBleeder processor = new FrameBleeder();

    InfoVideo videoInfo = new InfoVideo(
            "bleeder-test",
            frameCount,
            width,
            height,
            Frame.DEPTH_UBYTE,
            channels,
            width * channels,
            24.0,
            24,
            41667,
            0
    );

    processor.initialise(videoInfo);

    List<Frame> sequentialFrames = new ArrayList<>();
    List<Frame> concurrentFrames = new ArrayList<>();

    for (int i = 0; i < frameCount; i++) {
        Frame frame = new Frame(
                width, height, Frame.DEPTH_UBYTE, channels);

        ByteBuffer pixels = (ByteBuffer) frame.image[0];

        for (int j = 0; j < width * height * channels; j++) {
            pixels.put(j, (byte) ((i + j) % 256));
        }

        sequentialFrames.add(frame);
        concurrentFrames.add(frame.clone());
    }

    // Measure sequential processing.
    long sequentialStart = System.nanoTime();

    for (int i = 0; i < frameCount; i++) {
        processor.process(
                sequentialFrames.get(i),
                new InfoFrame(i, i * 41667L)
        );
    }

    long sequentialTime = System.nanoTime() - sequentialStart;

    ExecutorService executor = Executors.newFixedThreadPool(4);

    try {
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < frameCount; i++) {
            final int frameNumber = i;

            tasks.add(() -> {
                processor.process(
                        concurrentFrames.get(frameNumber),
                        new InfoFrame(frameNumber, frameNumber * 41667L)
                );
                return null;
            });
        }

        // Measure concurrent processing.
        long concurrentStart = System.nanoTime();

        for (Future<Void> result : executor.invokeAll(tasks)) {
            result.get();
        }

        long concurrentTime = System.nanoTime() - concurrentStart;

        System.out.println("Bleeder sequential time: "
                + sequentialTime / 1_000_000.0 + " ms");

        System.out.println("Bleeder concurrent time: "
                + concurrentTime / 1_000_000.0 + " ms");
        double speedup = (double) sequentialTime / concurrentTime;

        System.out.printf("Bleeder speedup: %.2fx%n", speedup);

        // Verify identical output pixels.
        for (int i = 0; i < frameCount; i++) {
            ByteBuffer expected =
                    (ByteBuffer) sequentialFrames.get(i).image[0];

            ByteBuffer actual =
                    (ByteBuffer) concurrentFrames.get(i).image[0];

            assertEquals(expected.capacity(), actual.capacity());

            for (int j = 0; j < expected.capacity(); j++) {
                assertEquals(
                        expected.get(j),
                        actual.get(j),
                        "Bleeder pixel mismatch at frame "
                                + i + ", byte " + j
                );
            }
        }

    } finally {
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(
                5, TimeUnit.SECONDS));
    }
}


}
