import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class View extends JPanel {
    // Logger untuk mencatat kegagalan pembacaan aset gambar/font tanpa menghentikan
    // thread GUI
    private static final Logger LOGGER = Logger.getLogger(View.class.getName());
    // Kunci identifikasi kartu layout untuk tampilan Main Menu
    private static final String CARD_MENU = "MENU";
    // Kunci identifikasi kartu layout untuk tampilan Kanvas Gameplay
    private static final String CARD_GAME = "GAME";

    // Manajer layout kartu untuk perpindahan mulus antara halaman menu dan halaman
    // game
    private final CardLayout cardLayout;
    // Panel khusus untuk rendering grafis permainan secara real-time
    private final GamePanel gamePanel;
    // Panel khusus untuk menampilkan menu pembuka, judul, dan tombol aksi
    private final MenuPanel menuPanel;

    // Menampung sprite karakter burung
    private Image birdImage;
    // Menampung latar belakang pemandangan kota dan langit
    private Image backgroundImage;
    // Menampung sprite pipa atas (menghadap ke bawah)
    private Image upperPipeImage;
    // Menampung sprite pipa bawah (menghadap ke atas)
    private Image lowerPipeImage;

    // Font kustom bertema arcade retro untuk judul header dan angka skor besar
    private Font arcadepixFont;
    // Font kustom bertema GameBoy retro untuk tombol antarmuka dan teks petunjuk
    private Font earlyGameBoyFont;

    // Referensi ke controller logika untuk pertukaran status permainan dan input
    private Logic logic;

    public View() {
        // Inisialisasi CardLayout sebagai pengatur transisi antar panel
        this.cardLayout = new CardLayout();
        // Tetapkan layout kartu ke container utama View
        setLayout(cardLayout);
        // Tetapkan resolusi kanvas standar Flappy Bird (360x640 px)
        setPreferredSize(new Dimension(Logic.BOARD_WIDTH, Logic.BOARD_HEIGHT));

        // Muat aset font kustom terlebih dahulu agar siap dipakai oleh komponen GUI
        loadFonts();
        // Muat seluruh aset gambar yang diperlukan sebelum komponen GUI dibentuk
        loadImages();

        // Buat instans panel halaman menu
        this.menuPanel = new MenuPanel();
        // Buat instans panel kanvas game
        this.gamePanel = new GamePanel();

        // Daftarkan panel menu ke CardLayout dengan pengenal CARD_MENU
        add(menuPanel, CARD_MENU);
        // Daftarkan panel gameplay ke CardLayout dengan pengenal CARD_GAME
        add(gamePanel, CARD_GAME);
    }

    public void init(Logic logic) {
        // Sambungkan referensi Logic ke View
        this.logic = logic;
        // Pasang event listener untuk tombol-tombol pada panel menu
        menuPanel.setupListeners();
        // Pasang event listener keyboard dan mouse pada panel gameplay
        gamePanel.setupListeners();
        // Tampilkan halaman Main Menu sebagai antarmuka awal
        showMenu();
    }

    private void loadFonts() {
        // Muat font Arcadepix Plus untuk header dan skor besar (ukuran dasar 32 pt)
        this.arcadepixFont = loadCustomFont("ARCADEPI.TTF", "Arial", 32f);
        // Muat font Early GameBoy untuk elemen UI, tombol, dan petunjuk (ukuran dasar
        // 14 pt)
        this.earlyGameBoyFont = loadCustomFont("Early GameBoy.ttf", "Arial", 14f);
    }

    private Font loadCustomFont(String fileName, String fallbackFamily, float fallbackSize) {
        try {
            InputStream stream = null;
            // Cari file font pada classpath internal
            URL resource = getClass().getResource("/assets/" + fileName);
            if (resource != null) {
                stream = resource.openStream();
            } else {
                // Fallback pencarian file font dari direktori relatif src/assets
                File srcFile = new File("src/assets/" + fileName);
                if (srcFile.exists()) {
                    stream = new FileInputStream(srcFile);
                } else {
                    // Fallback alternatif kedua dari direktori assets di root
                    File rootFile = new File("assets/" + fileName);
                    if (rootFile.exists()) {
                        stream = new FileInputStream(rootFile);
                    }
                }
            }

            if (stream != null) {
                try (InputStream is = stream) {
                    // Buat instance font TrueType dari stream data biner
                    Font font = Font.createFont(Font.TRUETYPE_FONT, is);
                    // Daftarkan font ke GraphicsEnvironment sistem agar rendering teks bekerja
                    // konsisten
                    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    ge.registerFont(font);
                    return font;
                }
            }
            LOGGER.log(Level.WARNING, "Custom font file not found: {0}", fileName);
        } catch (Exception e) {
            // Isolasi kegagalan pemuatan font kustom agar game tetap berjalan dengan
            // fallback
            LOGGER.log(Level.WARNING, "Failed to load custom font: " + fileName, e);
        }
        // Gunakan font cadangan sistem jika terjadi kendala saat memuat berkas TTF
        return new Font(fallbackFamily, Font.BOLD, (int) fallbackSize);
    }

    public Font getArcadepix(float size) {
        // Dapatkan salinan font Arcadepix Plus dengan ukuran point spesifik
        if (arcadepixFont != null) {
            return arcadepixFont.deriveFont(Font.PLAIN, size);
        }
        return new Font("Arial", Font.BOLD, (int) size);
    }

    public Font getEarlyGameBoy(float size) {
        // Dapatkan salinan font Early GameBoy dengan ukuran point spesifik
        if (earlyGameBoyFont != null) {
            return earlyGameBoyFont.deriveFont(Font.PLAIN, size);
        }
        return new Font("Arial", Font.BOLD, (int) size);
    }

    private void loadImages() {
        // Muat sprite burung dari berkas bird.png
        this.birdImage = loadImage("bird.png");
        // Muat latar belakang pemandangan dari berkas background.png
        this.backgroundImage = loadImage("background.png");
        // Muat gambar rintangan pipa atas dari berkas upperPipe.png
        this.upperPipeImage = loadImage("upperPipe.png");
        // Muat gambar rintangan pipa bawah dari berkas lowerPipe.png
        this.lowerPipeImage = loadImage("lowerPipe.png");
    }

    private Image loadImage(String fileName) {
        try {
            // Telusuri berkas gambar dari classpath Java (misal: saat dibungkus ke JAR atau
            // out compiler)
            URL resource = getClass().getResource("/assets/" + fileName);
            if (resource != null) {
                // Baca dan dekode gambar dari aliran URL resource
                return ImageIO.read(resource);
            }

            // Fallback: telusuri berkas dari path relatif src/assets
            File srcFile = new File("src/assets/" + fileName);
            if (srcFile.exists()) {
                // Baca dan dekode gambar dari berkas lokal di src/assets
                return ImageIO.read(srcFile);
            }

            // Fallback alternatif: telusuri berkas dari path direktori assets di root
            // proyek
            File rootFile = new File("assets/" + fileName);
            if (rootFile.exists()) {
                // Baca dan dekode gambar dari berkas lokal di direktori root assets
                return ImageIO.read(rootFile);
            }

            // Catat log jika berkas tidak ditemukan di semua jalur pencarian
            LOGGER.log(Level.WARNING, "Image asset not found: {0}", fileName);
            return null;
        } catch (Exception e) {
            // Isolasi kegagalan parsing format gambar agar program tidak berhenti tiba-tiba
            LOGGER.log(Level.WARNING, "Failed to load image asset: " + fileName, e);
            return null;
        }
    }

    public void showMenu() {
        // Sembunyikan tombol-tombol Game Over saat beralih ke menu
        gamePanel.hideGameOverButtons();
        // Pindahkan kartu aktif ke halaman Main Menu
        cardLayout.show(this, CARD_MENU);
        // Refresh tampilan menu agar menampilkan poin terbaik sesi terkini
        menuPanel.repaint();
    }

    public void showGame() {
        // Pastikan tombol Game Over dalam keadaan tersembunyi saat memulai game baru
        gamePanel.hideGameOverButtons();
        // Pindahkan kartu aktif ke halaman kanvas Gameplay
        cardLayout.show(this, CARD_GAME);
        // Alihkan fokus keyboard ke gamePanel secara asinkron di Event Dispatch Thread
        // agar tombol Spasi & R langsung aktif
        SwingUtilities.invokeLater(gamePanel::requestFocusInWindow);
    }

    public void repaintGame() {
        // Trigger pemanggilan ulang paintComponent pada GamePanel untuk merender posisi
        // frame terkini
        gamePanel.repaint();
    }

    public Image getBirdImage() {
        // Sediakan gambar sprite burung untuk entitas Player
        return birdImage;
    }

    public Image getUpperPipeImage() {
        // Sediakan gambar pipa atas untuk entitas Pipe
        return upperPipeImage;
    }

    public Image getLowerPipeImage() {
        // Sediakan gambar pipa bawah untuk entitas Pipe
        return lowerPipeImage;
    }

    public JButton createStyledButton(String text, Color background) {
        // Instansiasi JButton dengan teks yang ditentukan
        JButton button = new JButton(text);
        // Terapkan font Early GameBoy berukuran 12 pt untuk teks tombol UI sesuai
        // arahan
        button.setFont(getEarlyGameBoy(12f));
        // Ubah warna font teks tombol menjadi HITAM pekat sesuai arahan pengguna
        button.setForeground(Color.BLACK);
        // Atur warna latar belakang tombol dengan warna pastel yang kontras dengan teks
        // hitam
        button.setBackground(background);
        // Hilangkan garis putus-putus bawaan Swing saat tombol diklik
        button.setFocusPainted(false);
        // Matikan kemampuan fokus tombol agar penekanan Spasi/R tetap diterima oleh
        // GamePanel
        button.setFocusable(false);
        // Ubah bentuk kursor menjadi tangan saat melayang di atas tombol
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        // Tentukan dimensi standar tombol (lebar 220 px cukup untuk teks terpanjang)
        button.setPreferredSize(new Dimension(220, 42));
        // Kembalikan tombol yang telah didekorasi
        return button;
    }

    private class MenuPanel extends JPanel {
        // Tombol interaktif untuk mengawali ronde permainan
        private final JButton startButton;
        // Tombol interaktif untuk menghentikan dan keluar dari aplikasi
        private final JButton exitButton;

        public MenuPanel() {
            // Pastikan ukuran panel menu konsisten dengan ukuran frame
            setPreferredSize(new Dimension(Logic.BOARD_WIDTH, Logic.BOARD_HEIGHT));
            // Gunakan GridBagLayout agar posisi komponen tombol dapat diatur secara presisi
            // di tengah
            setLayout(new GridBagLayout());

            // Buat tombol 'Mulai Game' bertema warna hijau muda terang (kontras tinggi
            // dengan teks hitam)
            this.startButton = createStyledButton("Mulai Game", new Color(129, 212, 137));
            // Buat tombol 'Keluar' bertema warna salmon / koral terang
            this.exitButton = createStyledButton("Keluar", new Color(255, 138, 128));

            // Buat objek konfigurasi tata letak GridBag
            GridBagConstraints gbc = new GridBagConstraints();
            // Atur padding margin antar tombol
            gbc.insets = new Insets(8, 20, 8, 20);
            // Letakkan tombol pada kolom indeks ke-0
            gbc.gridx = 0;
            // Buat lebar tombol membentang mengikuti batas preferensi
            gbc.fill = GridBagConstraints.HORIZONTAL;

            // Baris ke-0 untuk tombol Mulai Game
            gbc.gridy = 0;
            // Beri margin atas lebih besar agar tombol berada di bawah judul dan indikator
            // skor terbaik
            gbc.insets = new Insets(195, 20, 10, 20);
            // Pasang tombol Mulai Game ke dalam panel
            add(startButton, gbc);

            // Baris ke-1 untuk tombol Keluar
            gbc.gridy = 1;
            // Beri margin proporsional untuk tombol kedua
            gbc.insets = new Insets(6, 20, 20, 20);
            // Pasang tombol Keluar ke dalam panel
            add(exitButton, gbc);
        }

        public void setupListeners() {
            // Daftarkan listener aksi klik untuk tombol Mulai Game
            startButton.addActionListener(e -> {
                if (logic != null) {
                    // Jalankan siklus permainan baru
                    logic.startGame();
                }
            });

            // Daftarkan listener aksi klik tombol Keluar untuk mengakhiri program
            exitButton.addActionListener(e -> System.exit(0));
        }

        @Override
        protected void paintComponent(Graphics g) {
            // Bersihkan kanvas bawaan JPanel
            super.paintComponent(g);
            // Konversikan Graphics ke Graphics2D untuk memanfaatkan fitur rendering modern
            Graphics2D g2d = (Graphics2D) g;
            // Aktifkan antialiasing bentuk geometri untuk visualisasi yang halus
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Aktifkan antialiasing teks agar font terbaca tajam
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Gambar latar belakang menu
            if (backgroundImage != null) {
                // Skalakan gambar latar belakang memenuhi seluruh ukuran panel
                g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);
            } else {
                // Fallback warna biru langit jika gambar latar gagal dimuat
                g2d.setColor(new Color(78, 192, 202));
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }

            // Teks judul utama game menggunakan font retro Arcadepix Plus (34 pt)
            String title = "FLAPPY BIRD";
            Font titleFont = getArcadepix(34f);
            g2d.setFont(titleFont);
            // Dapatkan ukuran teks untuk menempatkan judul tepat di tengah kanvas secara
            // horizontal
            FontMetrics fm = g2d.getFontMetrics(titleFont);
            int titleX = (getWidth() - fm.stringWidth(title)) / 2;
            int titleY = 125;

            // Render bayangan hitam (drop shadow) judul untuk meningkatkan keterbacaan
            g2d.setColor(new Color(0, 0, 0, 160));
            g2d.drawString(title, titleX + 3, titleY + 3);
            // Render warna teks judul utama (kuning emas)
            g2d.setColor(new Color(255, 215, 0));
            g2d.drawString(title, titleX, titleY);

            // Render preview karakter burung di bawah judul
            if (birdImage != null) {
                // Posisikan preview burung di bagian tengah secara horizontal
                int birdX = (getWidth() - Player.WIDTH * 2) / 2;
                int birdY = 145;
                // Render gambar burung dengan perbesaran skala 2x agar proporsional sebagai
                // ikon menu
                g2d.drawImage(birdImage, birdX, birdY, Player.WIDTH * 2, Player.HEIGHT * 2, null);
            }

            // Tampilkan informasi poin terbaik sesi ini di Main Menu menggunakan font Early
            // GameBoy (13 pt)
            if (logic != null && logic.getHighScore() > 0) {
                String bestText = "Poin Terbaik Sesi Ini: " + logic.getHighScore();
                Font bestFont = getEarlyGameBoy(13f);
                g2d.setFont(bestFont);
                FontMetrics fmBest = g2d.getFontMetrics(bestFont);
                int bestX = (getWidth() - fmBest.stringWidth(bestText)) / 2;
                int bestY = 225;

                g2d.setColor(new Color(0, 0, 0, 160));
                g2d.drawString(bestText, bestX + 1, bestY + 1);
                g2d.setColor(new Color(255, 223, 0));
                g2d.drawString(bestText, bestX, bestY);
            }
        }
    }

    private class GamePanel extends JPanel {
        // Tombol interaktif untuk mengulang permainan saat kalah
        private final JButton tryAgainButton;
        // Tombol untuk kembali ke menu utama
        private final JButton menuButton;

        public GamePanel() {
            // Tentukan dimensi tetap kanvas permainan
            setPreferredSize(new Dimension(Logic.BOARD_WIDTH, Logic.BOARD_HEIGHT));
            // Gunakan absolute positioning layout agar tombol Game Over dapat diposisikan
            // tepat di tengah
            setLayout(null);
            // Aktifkan kapabilitas penerimaan fokus keyboard pada panel gameplay
            setFocusable(true);

            // Inisialisasi tombol Try Again bertema hijau muda (teks hitam)
            this.tryAgainButton = createStyledButton("Try Again", new Color(129, 212, 137));
            // Inisialisasi tombol Kembali ke Menu bertema biru langit terang (teks hitam)
            this.menuButton = createStyledButton("Kembali ke Menu", new Color(129, 212, 250));

            // Posisikan tombol-tombol pada area tengah vertikal di bawah teks informasi
            // Game Over
            int buttonWidth = 220;
            int buttonX = (Logic.BOARD_WIDTH - buttonWidth) / 2;
            tryAgainButton.setBounds(buttonX, 350, buttonWidth, 44);
            menuButton.setBounds(buttonX, 410, buttonWidth, 44);

            // Sembunyikan tombol-tombol saat inisialisasi awal (hanya muncul ketika Game
            // Over)
            hideGameOverButtons();

            // Pasang tombol-tombol ke panel GamePanel
            add(tryAgainButton);
            add(menuButton);
        }

        public void hideGameOverButtons() {
            // Nonaktifkan visibilitas tombol kontrol ronde permainan
            tryAgainButton.setVisible(false);
            menuButton.setVisible(false);
        }

        public void setupListeners() {
            // Aksi tombol Try Again: restart ronde permainan baru
            tryAgainButton.addActionListener(e -> {
                if (logic != null) {
                    logic.handleRestart();
                }
            });

            // Aksi tombol Kembali ke Menu: hentikan game dan kembali ke tampilan Main Menu
            menuButton.addActionListener(e -> {
                if (logic != null) {
                    logic.returnToMenu();
                }
            });

            // Pasang KeyListener untuk merespon interaksi keyboard pemain
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (logic == null) {
                        return;
                    }
                    // Deteksi penekanan tombol Spasi untuk melompat
                    if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                        // Teruskan aksi lompat ke controller Logic
                        logic.handleJump();
                        // Deteksi penekanan tombol R (huruf besar/kecil) sebagai shortcut restart
                        // alternatif
                    } else if (e.getKeyCode() == KeyEvent.VK_R || e.getKeyChar() == 'r' || e.getKeyChar() == 'R') {
                        // Teruskan aksi restart ke controller Logic
                        logic.handleRestart();
                    }
                }
            });

            // Pasang MouseListener sebagai kontrol alternatif dan penjaga fokus keyboard
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    // Paksa panel mengambil fokus keyboard ketika pemain mengeklik layar
                    requestFocusInWindow();
                    if (logic != null && logic.isGameStarted() && !logic.isGameOver()) {
                        // Picu lompatan burung saat layar diklik mouse dalam fase aktif
                        logic.handleJump();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            // Bersihkan kanvas bawaan JPanel
            super.paintComponent(g);
            // Konversikan Graphics ke Graphics2D untuk akses rendering mutakhir
            Graphics2D g2d = (Graphics2D) g;
            // Nyalakan antialiasing rendering grafis
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Nyalakan antialiasing rendering teks
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 1. Render gambar background
            if (backgroundImage != null) {
                // Gambar background memenuhi seluruh jendela permainan
                g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);
            } else {
                // Fallback kanvas warna biru jika file background hilang
                g2d.setColor(new Color(78, 192, 202));
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }

            // Jika controller logic belum terpasang, hentikan rendering lapisan game
            if (logic == null) {
                return;
            }

            // 2. Render seluruh rintangan pipa yang terdaftar
            for (Pipe pipe : logic.getPipes()) {
                if (pipe.getImage() != null) {
                    // Gambar sprite pipa sesuai posisi koordinat dan dimensinya
                    g2d.drawImage(pipe.getImage(), pipe.getX(), pipe.getY(), pipe.getWidth(), pipe.getHeight(), null);
                } else {
                    // Fallback balok warna hijau jika sprite pipa tidak termuat
                    g2d.setColor(new Color(34, 139, 34));
                    g2d.fillRect(pipe.getX(), pipe.getY(), pipe.getWidth(), pipe.getHeight());
                }
            }

            // 3. Render entitas burung (Player)
            Player player = logic.getPlayer();
            if (player != null) {
                if (player.getImage() != null) {
                    // Gambar sprite burung pada koordinat terkini
                    g2d.drawImage(player.getImage(), player.getX(), player.getY(), player.getWidth(),
                            player.getHeight(), null);
                } else {
                    // Fallback bentuk lingkaran kuning jika gambar sprite burung belum terpasang
                    g2d.setColor(Color.YELLOW);
                    g2d.fillOval(player.getX(), player.getY(), player.getWidth(), player.getHeight());
                }
            }

            // 4. Render teks skor secara real-time pada pojok kiri atas menggunakan font
            // Arcadepix Plus (28 pt)
            String scoreText = "Score: " + logic.getScore();
            g2d.setFont(getArcadepix(28f));
            // Gambar bayangan teks berwarna hitam untuk keterbacaan tinggi
            g2d.setColor(Color.BLACK);
            g2d.drawString(scoreText, 22, 47);
            // Gambar teks skor berwarna putih di atas bayangan
            g2d.setColor(Color.WHITE);
            g2d.drawString(scoreText, 20, 45);

            // Tampilkan petunjuk di layar saat game berada pada mode siaga (sebelum tombol Spasi ditekan)
            if (logic.isWaitingToStart()) {
                String startHint = "Tekan SPASI untuk Mulai";
                Font hintFont = getEarlyGameBoy(13f);
                g2d.setFont(hintFont);
                FontMetrics fmHint = g2d.getFontMetrics(hintFont);
                int hintX = (getWidth() - fmHint.stringWidth(startHint)) / 2;
                int hintY = 385;

                // Bayangan teks untuk keterbacaan kontras di atas background langit
                g2d.setColor(new Color(0, 0, 0, 180));
                g2d.drawString(startHint, hintX + 1, hintY + 1);
                // Teks putih bersih
                g2d.setColor(Color.WHITE);
                g2d.drawString(startHint, hintX, hintY);
            }

            // 5. Render lapisan antarmuka saat kondisi Game Over tercapai
            if (logic.isGameOver()) {
                // Aktifkan visibilitas tombol interaktif Game Over (Try Again dan Kembali ke
                // Menu)
                tryAgainButton.setVisible(true);
                menuButton.setVisible(true);

                // Gambar overlay hitam semi-transparan (alpha = 165) untuk menggelapkan latar
                // gameplay
                g2d.setColor(new Color(0, 0, 0, 165));
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Teks penanda kekalahan menggunakan font Arcadepix Plus (34 pt)
                String goText = "GAME OVER";
                Font goFont = getArcadepix(34f);
                g2d.setFont(goFont);
                // Ukur lebar teks agar berada tepat di tengah kanvas
                FontMetrics fmGo = g2d.getFontMetrics(goFont);
                int goX = (getWidth() - fmGo.stringWidth(goText)) / 2;
                int goY = 180;

                // Gambar bayangan teks Game Over
                g2d.setColor(Color.BLACK);
                g2d.drawString(goText, goX + 2, goY + 2);
                // Gambar teks Game Over dengan warna oranye menyala
                g2d.setColor(new Color(255, 69, 0));
                g2d.drawString(goText, goX, goY);

                // Teks akumulasi skor akhir ronde ini menggunakan font Arcadepix Plus (28 pt)
                String finalScoreText = "Skor Akhir: " + logic.getScore();
                Font scoreFont = getArcadepix(28f);
                g2d.setFont(scoreFont);
                FontMetrics fmScore = g2d.getFontMetrics(scoreFont);
                int scoreX = (getWidth() - fmScore.stringWidth(finalScoreText)) / 2;
                int scoreY = 230;

                // Gambar bayangan teks skor akhir
                g2d.setColor(Color.BLACK);
                g2d.drawString(finalScoreText, scoreX + 1, scoreY + 1);
                // Gambar teks skor akhir dengan warna putih bersih
                g2d.setColor(Color.WHITE);
                g2d.drawString(finalScoreText, scoreX, scoreY);

                // Teks keterangan poin terbaik sepanjang sesi permainan menggunakan font Early
                // GameBoy (14 pt)
                String bestScoreText = "Poin Terbaik: " + logic.getHighScore();
                Font bestFont = getEarlyGameBoy(14f);
                g2d.setFont(bestFont);
                FontMetrics fmBest = g2d.getFontMetrics(bestFont);
                int bestX = (getWidth() - fmBest.stringWidth(bestScoreText)) / 2;
                int bestY = 275;

                // Gambar bayangan teks poin terbaik
                g2d.setColor(Color.BLACK);
                g2d.drawString(bestScoreText, bestX + 1, bestY + 1);
                // Gambar teks poin terbaik dengan warna kuning emas
                g2d.setColor(new Color(255, 215, 0));
                g2d.drawString(bestScoreText, bestX, bestY);

                // Jika ronde ini menghasilkan atau menyamai rekor tertinggi baru, tampilkan
                // apresiasi (12 pt Early GameBoy)
                if (logic.getScore() >= logic.getHighScore() && logic.getScore() > 0) {
                    String recordText = "★ REKOR BARU TERCAPAI! ★";
                    Font recordFont = getEarlyGameBoy(12f);
                    g2d.setFont(recordFont);
                    FontMetrics fmRecord = g2d.getFontMetrics(recordFont);
                    int recordX = (getWidth() - fmRecord.stringWidth(recordText)) / 2;
                    int recordY = 310;

                    g2d.setColor(Color.BLACK);
                    g2d.drawString(recordText, recordX + 1, recordY + 1);
                    g2d.setColor(new Color(50, 205, 50));
                    g2d.drawString(recordText, recordX, recordY);
                }
            } else {
                // Sembunyikan tombol-tombol saat game aktif berlangsung
                hideGameOverButtons();
            }
        }
    }
}
