package compTest3011;

import comp3011.CommandLineController;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CommandLineControllerTest {

    @TempDir
    Path tempDir;

    private String createTestVideo() throws IOException {
        return Files.createFile(
            tempDir.resolve("test.mp4")
        ).toString();
    }

    @Test
    void testAllEffects() throws IOException {

        String video = createTestVideo();

        String[] effects = {
            "n", "s", "f", "w", "y", "v",
            "d", "j", "m", "b", "p"
        };

        for (String effect : effects) {

            CommandLineController controller =
                new CommandLineController(
                    new String[]{"-" + effect, video}
                );

            assertEquals(
                List.of(effect),
                controller.getRequestedEffects()
            );
        }
    }

    @Test
    void testCombinedEffects() throws IOException {

        String video = createTestVideo();

        CommandLineController controller =
            new CommandLineController(
                new String[]{"-nsf", video}
            );

        assertEquals(
            List.of("n", "s", "f"),
            controller.getRequestedEffects()
        );
    }

    @Test
    void testRepeatedEffects() throws IOException {

        String video = createTestVideo();

        CommandLineController controller =
            new CommandLineController(
                new String[]{"-ssn", video}
            );

        assertEquals(
            List.of("s", "s", "n"),
            controller.getRequestedEffects()
        );
    }

    @Test
    void testEffectOrder() throws IOException {

        String video = createTestVideo();

        CommandLineController controller =
            new CommandLineController(
                new String[]{"-fsn", video}
            );

        assertEquals(
            List.of("f", "s", "n"),
            controller.getRequestedEffects()
        );
    }

    @Test
    void testAudioOption() throws IOException {

        String video = createTestVideo();

        CommandLineController controller =
            new CommandLineController(
                new String[]{"-ans", video}
            );

        assertTrue(controller.isAudioRequested());

        assertEquals(
            List.of("n", "s"),
            controller.getRequestedEffects()
        );
    }
}
