import java.io.File;
import javax.sound.sampled.*;

public class SoundManager {

    final private Clip deliverySuccess;
    final private Clip orderExpiry;
    final private Clip gameOver;

    SoundManager() {
        deliverySuccess = load("assets/delivery_success.wav");
        orderExpiry     = load("assets/order_expiry.wav");
        gameOver        = load("assets/game_over.wav");
    }

    private Clip load(String path) {
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(new File(path));
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            return clip;
        } catch (Exception ex) {
            System.out.println("Sound not found: " + path);
            return null;
        }
    }

    private void play(Clip clip) {
        if (clip == null) return;
        clip.setFramePosition(0);
        clip.start();
    }

    void playDeliverySuccess() { play(deliverySuccess); }
    void playOrderExpiry()     { play(orderExpiry);     }
    void playGameOver()        { play(gameOver);        }
}
