import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class SoundManager {
    // Logger internal untuk mencatat kegagalan audio tanpa menghentikan alur eksekusi aplikasi
    private static final Logger LOGGER = Logger.getLogger(SoundManager.class.getName());

    // Objek Clip penyimpan audio musik latar yang berjalan berulang (loop)
    private final Clip bgmClip;
    // Objek Clip efek suara saat burung mengepakkan sayap atau melompat
    private final Clip jumpClip;
    // Objek Clip efek suara saat burung menabrak rintangan atau terjatuh
    private final Clip hitClip;
    // Objek Clip efek suara saat burung berhasil melewati rintangan dan mencetak skor
    private final Clip pointClip;

    public SoundManager() {
        // Muat musik latar dari file audio bgm.wav
        this.bgmClip = loadClip("bgm.wav");
        // Muat efek suara lompat dari file audio jump.wav
        this.jumpClip = loadClip("jump.wav");
        // Muat efek suara benturan dari file audio hit.wav
        this.hitClip = loadClip("hit.wav");
        // Muat efek suara perolehan skor dari file audio point.wav
        this.pointClip = loadClip("point.wav");
    }

    private Clip loadClip(String fileName) {
        try {
            // Deklarasikan variabel input stream untuk membaca byte audio
            InputStream stream = null;
            // Cari lokasi berkas audio melalui classpath internal (mendukung kemasan JAR / out direktori)
            URL resource = getClass().getResource("/assets/" + fileName);

            if (resource != null) {
                // Buka aliran data dari resource URL jika ditemukan pada classpath
                stream = resource.openStream();
            } else {
                // Fallback pencarian berkas dari struktur direktori proyek relatif src/assets
                File srcFile = new File("src/assets/" + fileName);
                if (srcFile.exists()) {
                    // Buat file stream dari path direktori src/assets
                    stream = new FileInputStream(srcFile);
                } else {
                    // Fallback alternatif kedua dari direktori assets pada level root proyek
                    File rootFile = new File("assets/" + fileName);
                    if (rootFile.exists()) {
                        // Buat file stream dari path direktori root assets
                        stream = new FileInputStream(rootFile);
                    }
                }
            }

            // Validasi apakah berkas audio berhasil ditemukan pada salah satu lokasi pencarian
            if (stream == null) {
                // Beri peringatan pada log jika file audio tidak ditemukan, namun biarkan game tetap berjalan
                LOGGER.log(Level.WARNING, "Audio resource not found: {0}", fileName);
                return null;
            }

            // Bungkus dalam BufferedInputStream agar mendukung operasi mark dan reset yang diwajibkan AudioSystem
            InputStream bufferedStream = new BufferedInputStream(stream);
            // Buka AudioInputStream dengan blok try-with-resources untuk pembersihan alokasi stream secara otomatis
            try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedStream)) {
                // Minta resource baris audio Clip baru dari sistem audio perangkat keras
                Clip clip = AudioSystem.getClip();
                // Buka dan muat sampel audio ke dalam memori kerja Clip
                clip.open(audioStream);
                // Kembalikan objek Clip yang telah terisi data audio
                return clip;
            }
        } catch (Exception e) {
            // Isolasi seluruh exception audio agar kerusakan file atau driver suara tidak merusak gameplay
            LOGGER.log(Level.WARNING, "Unable to initialize audio file: " + fileName, e);
            // Kembalikan null sebagai tanda bahwa klip suara tidak tersedia
            return null;
        }
    }

    public void playBGM() {
        // Abaikan pemutaran jika aset audio musik latar gagal dimuat sebelumnya
        if (bgmClip == null) {
            return;
        }
        try {
            // Kembalikan posisi pemutaran ke awal (frame ke-0)
            bgmClip.setFramePosition(0);
            // Jalankan pemutaran secara berulang tanpa henti (loop)
            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception e) {
            // Tangkap potensi kegagalan thread audio saat memutar BGM
            LOGGER.log(Level.FINE, "Failed to play BGM", e);
        }
    }

    public void stopBGM() {
        // Abaikan perintah berhenti jika musik latar memang tidak diinisialisasi
        if (bgmClip == null) {
            return;
        }
        try {
            // Hentikan pemutaran jika klip audio saat ini sedang aktif bersuara
            if (bgmClip.isRunning()) {
                bgmClip.stop();
            }
            // Reset pointer audio kembali ke detik awal agar siap jika diputar kembali
            bgmClip.setFramePosition(0);
        } catch (Exception e) {
            // Tangkap potensi kendala saat memutus stream BGM
            LOGGER.log(Level.FINE, "Failed to stop BGM", e);
        }
    }

    public void playJump() {
        // Mainkan efek suara lompatan saat tombol kendali ditekan
        playSound(jumpClip);
    }

    public void playHit() {
        // Mainkan efek suara tabrakan saat terjadi benturan atau terjatuh
        playSound(hitClip);
    }

    public void playPoint() {
        // Mainkan efek suara skor saat berhasil melewati celah rintangan
        playSound(pointClip);
    }

    private void playSound(Clip clip) {
        // Cek ketersediaan objek klip efek suara
        if (clip == null) {
            return;
        }
        try {
            // Jika klip sebelumnya masih berbunyi, potong segera untuk memainkan efek yang baru
            if (clip.isRunning()) {
                clip.stop();
            }
            // Kembalikan posisi pembacaan frame audio ke awal
            clip.setFramePosition(0);
            // Mulai memutar efek suara satu kali (non-looping)
            clip.start();
        } catch (Exception e) {
            // Tangkap error jika terjadi konflik pada buffer audio internal sistem operasi
            LOGGER.log(Level.FINE, "Failed to play sound effect", e);
        }
    }
}
