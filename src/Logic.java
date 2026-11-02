import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Timer;

public class Logic implements ActionListener {
    // Lebar viewport bidang permainan dalam piksel
    public static final int BOARD_WIDTH = 360;
    // Tinggi viewport bidang permainan dalam piksel
    public static final int BOARD_HEIGHT = 640;
    // Jarak celah vertikal antar pipa (seperempat tinggi layar = 640 / 4) agar adil
    // dilewati burung
    public static final int OPENING_SPACE = 160;

    // Entitas burung yang dikendalikan oleh pemain
    private final Player player;
    // Koleksi dinamis untuk menampung seluruh rintangan pipa aktif di layar
    private final List<Pipe> pipes;
    // Timer penggerak utama siklus kalkulasi fisika dan pembaruan grafis game
    private final Timer gameLoop;
    // Timer berkala untuk memunculkan pasangan pipa baru ke bidang permainan
    private final Timer placePipesTimer;
    // Pengelola efek suara dan musik latar permainan
    private final SoundManager soundManager;
    // Referensi ke lapisan antarmuka pengguna untuk koordinasi transisi dan repaint
    // kanvas
    private View view;

    // Penghitung jumlah skor rintangan yang berhasil dilewati pada ronde saat ini
    private int score;
    // Rekor poin terbaik (high score) yang pernah diraih selama sesi aplikasi
    // berjalan
    private int highScore;
    // Flag penanda apakah ronde permainan telah berakhir karena benturan atau jatuh
    private boolean gameOver;
    // Flag penanda apakah ronde permainan sedang aktif berjalan
    private boolean gameStarted;
    // Flag penanda apakah game berada dalam kondisi siaga menunggu penekanan tombol
    // spasi pertama
    private boolean waitingToStart;

    public Logic(View view, SoundManager soundManager) {
        // Simpan referensi ke antarmuka pengguna
        this.view = view;
        // Simpan referensi ke pengelola audio
        this.soundManager = soundManager;
        // Buat objek Player dengan menyematkan gambar sprite burung jika view telah
        // memuatnya
        this.player = new Player(view != null ? view.getBirdImage() : null);
        // Alokasikan memori penampung daftar pipa
        this.pipes = new ArrayList<>();
        // Inisialisasi poin terbaik awal sesi dengan nilai nol
        this.highScore = 0;
        this.gameStarted = false;
        this.waitingToStart = false;
        this.gameOver = false;

        // Konfigurasi Timer 60 FPS (~16.6 ms per tick) dengan listener kelas ini
        // sendiri
        this.gameLoop = new Timer(1000 / 60, this);

        // Konfigurasi Timer spawner rintangan pipa setiap 1500 ms (1.5 detik)
        this.placePipesTimer = new Timer(1500, this);
    }

    public void setView(View view) {
        // Kaitkan tampilan View jika diinisialisasi setelah konstruktor
        this.view = view;
        // Sinkronisasikan aset gambar ke entitas Player jika sebelumnya belum terpasang
        if (view != null && player.getImage() == null) {
            player.setImage(view.getBirdImage());
        }
    }

    public void startGame() {
        // Alihkan state ke mode siaga menunggu pemain menekan spasi untuk melompat
        gameStarted = false;
        waitingToStart = true;
        // Pastikan kondisi Game Over dalam posisi nonaktif
        gameOver = false;
        // Kembalikan posisi burung ke titik awal di tengah layar
        player.reset();
        // Bersihkan sisa pipa dari sesi permainan sebelumnya
        pipes.clear();
        // Reset akumulasi skor ronde berjalan kembali ke angka nol
        score = 0;

        // Pastikan timer game loop dan spawner rintangan belum berjalan agar burung
        // tidak jatuh
        gameLoop.stop();
        placePipesTimer.stop();
        // Hentikan musik pengiring jika sebelumnya aktif
        soundManager.stopBGM();

        // Alihkan layar antarmuka dari Main Menu ke kanvas Game
        if (view != null) {
            view.showGame();
            view.repaintGame();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Pilah sumber pemicu event berdasarkan objek Timer yang memanggilnya
        if (e.getSource() == gameLoop) {
            // Jalankan siklus pembaruan fisika dan deteksi benturan per frame
            update();
        } else if (e.getSource() == placePipesTimer) {
            // Jalankan penambahan rintangan pipa baru setiap interval 1500 ms tercapai
            placePipes();
        }
    }

    public void update() {
        // Jangan lakukan kalkulasi gerakan jika permainan sedang berhenti atau belum
        // dimulai
        if (gameOver || !gameStarted) {
            return;
        }

        // Terapkan kalkulasi gravitasi dan posisi Y pada burung
        player.move();

        // Deteksi tabrakan jika bagian bawah burung menyentuh batas bawah frame (lantai
        // kanvas)
        if (player.getY() + player.getHeight() >= BOARD_HEIGHT || player.getY() >= BOARD_HEIGHT) {
            // Hentikan permainan seketika saat burung menyentuh tanah
            triggerGameOver();
            return;
        }

        // Iterasi dan periksa status setiap pipa yang sedang meluncur di layar
        for (Pipe pipe : pipes) {
            // Geser posisi X pipa ke arah kiri layar
            pipe.move();

            // Evaluasi penambahan skor: hanya periksa pipa atas untuk menghindari double
            // count per pasangan
            if (!pipe.isPassed() && pipe.isUpper() && player.getX() > pipe.getX() + pipe.getWidth()) {
                // Tandai bahwa pasangan pipa ini telah tuntas dilewati
                pipe.setPassed(true);
                // Tingkatkan skor pemain sebanyak satu poin
                score++;
                // Mainkan bunyi efek skor berhasil didapat
                soundManager.playPoint();
            }

            // Periksa persilangan bounding box (AABB) antara pemain dan pipa rintangan
            if (player.getBounds().intersects(pipe.getBounds())) {
                // Hentikan permainan jika terjadi benturan fisik
                triggerGameOver();
                return;
            }
        }

        // Hapus pipa yang sudah bergeser keluar dari sisi kiri layar untuk mencegah
        // memory leak
        pipes.removeIf(pipe -> pipe.getX() + pipe.getWidth() < 0);

        // Minta antarmuka Swing untuk menggambar ulang kanvas dengan koordinat terbaru
        if (view != null) {
            view.repaintGame();
        }
    }

    public void placePipes() {
        // Jangan memunculkan rintangan baru jika ronde sedang berakhir atau belum aktif
        if (gameOver || !gameStarted) {
            return;
        }

        // Hitung posisi Y pipa atas secara acak dengan batas agar celah pembuka selalu
        // bisa dilewati
        int randomPipeY = (int) (0 - Pipe.HEIGHT / 4.0 - Math.random() * (Pipe.HEIGHT / 2.0));

        // Buat instans pipa atas yang menghadap ke bawah
        Pipe upperPipe = new Pipe(BOARD_WIDTH, randomPipeY, view != null ? view.getUpperPipeImage() : null, true);
        // Buat instans pipa bawah dengan posisi tepat di bawah pipa atas ditambah jarak
        // celah openingSpace
        Pipe lowerPipe = new Pipe(BOARD_WIDTH, randomPipeY + Pipe.HEIGHT + OPENING_SPACE,
                view != null ? view.getLowerPipeImage() : null, false);

        // Daftarkan pipa atas ke dalam daftar rintangan aktif
        pipes.add(upperPipe);
        // Daftarkan pipa bawah ke dalam daftar rintangan aktif
        pipes.add(lowerPipe);
    }

    public void triggerGameOver() {
        // Ubah flag state menjadi Game Over
        gameOver = true;
        gameStarted = false;
        waitingToStart = false;
        // Matikan timer pergerakan game loop
        gameLoop.stop();
        // Matikan timer pemuncul rintangan pipa
        placePipesTimer.stop();
        // Hentikan musik pengiring permainan
        soundManager.stopBGM();
        // Mainkan efek suara benturan / kekalahan
        soundManager.playHit();

        // Simpan rekor skor tertinggi baru jika skor saat ini melampaui rekor
        // sebelumnya
        if (score > highScore) {
            highScore = score;
        }

        // Minta kanvas menampilkan overlay Game Over dan skor akhir
        if (view != null) {
            view.repaintGame();
        }
    }

    public void restart() {
        // Tombol restart hanya boleh dieksekusi saat status permainan memang Game Over
        if (!gameOver) {
            return;
        }

        // Amankan skor tertinggi terlebih dahulu sebelum skor ronde di-reset
        if (score > highScore) {
            highScore = score;
        }

        // Kembalikan posisi dan kecepatan burung ke titik mula
        player.reset();
        // Hapus seluruh pipa yang tersisa di layar
        pipes.clear();
        // Nol-kan kembali nilai skor untuk ronde berikutnya
        score = 0;
        // Nonaktifkan status Game Over dan beralih ke mode siaga menunggu spasi
        gameOver = false;
        gameStarted = false;
        waitingToStart = true;

        // Pastikan timer game loop dan spawner pipa nonaktif agar burung tidak langsung
        // jatuh
        gameLoop.stop();
        placePipesTimer.stop();
        soundManager.stopBGM();

        // Segarkan kanvas untuk menampilkan posisi awal burung dan menyembunyikan
        // tombol Game Over
        if (view != null) {
            view.showGame();
            view.repaintGame();
        }
    }

    public void returnToMenu() {
        // Catat skor tertinggi jika melampaui rekor sesi sebelum kembali ke menu
        if (score > highScore) {
            highScore = score;
        }

        // Matikan siklus game loop dan spawner pipa
        gameLoop.stop();
        placePipesTimer.stop();
        // Matikan audio latar
        soundManager.stopBGM();

        // Kembalikan state internal game ke kondisi awal
        gameOver = false;
        gameStarted = false;
        waitingToStart = false;
        player.reset();
        pipes.clear();
        score = 0;

        // Alihkan antarmuka pengguna kembali ke kartu Main Menu
        if (view != null) {
            view.showMenu();
        }
    }

    public void handleJump() {
        // Jika sedang dalam mode siaga, spasi pertama memulai game dan membuat burung
        // melompat
        if (waitingToStart) {
            waitingToStart = false;
            gameStarted = true;
            // Jalankan game loop dan spawner pipa
            gameLoop.start();
            placePipesTimer.start();
            // Putar musik latar pengiring permainan
            soundManager.playBGM();
            // Berikan impuls lompatan pertama pada burung
            player.jump();
            soundManager.playJump();
            if (view != null) {
                view.repaintGame();
            }
            return;
        }

        // Aksi lompat reguler saat game sedang berlangsung aktif
        if (gameStarted && !gameOver) {
            player.jump();
            // Mainkan efek suara kepakan sayap
            soundManager.playJump();
        }
    }

    public void handleRestart() {
        // Panggil metode reset state permainan jika kondisi game saat ini sedang Game
        // Over
        if (gameOver) {
            restart();
        }
    }

    public Player getPlayer() {
        // Sediakan akses entitas burung bagi kelas View untuk digambar pada kanvas
        return player;
    }

    public List<Pipe> getPipes() {
        // Sediakan daftar rintangan pipa bagi kelas View untuk digambar pada kanvas
        return pipes;
    }

    public int getScore() {
        // Ambil nilai perolehan skor pemain saat ini
        return score;
    }

    public int getHighScore() {
        // Ambil rekor skor tertinggi yang berhasil dicapai selama sesi permainan
        // berjalan
        return highScore;
    }

    public boolean isGameOver() {
        // Cek status apakah kondisi Game Over sedang berlangsung
        return gameOver;
    }

    public boolean isGameStarted() {
        // Cek status apakah game sedang aktif berjalan
        return gameStarted;
    }

    public boolean isWaitingToStart() {
        // Cek status apakah game sedang siaga menunggu penekanan spasi pertama
        return waitingToStart;
    }

    public int getBoardWidth() {
        // Ambil batas lebar kanvas permainan
        return BOARD_WIDTH;
    }

    public int getBoardHeight() {
        // Ambil batas tinggi kanvas permainan
        return BOARD_HEIGHT;
    }
}
