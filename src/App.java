import java.awt.EventQueue;
import javax.swing.JFrame;

public class App {
    public static void main(String[] args) {
        // Alihkan inisialisasi antarmuka pengguna ke Event Dispatch Thread (EDT) demi keamanan thread GUI Swing
        EventQueue.invokeLater(App::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        // Buat jendela aplikasi tingkat atas (top-level container) dengan judul permainan
        JFrame frame = new JFrame("Flappy Bird");
        // Konfigurasikan agar proses Java langsung berhenti saat jendela utama ditutup oleh pengguna
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Kunci ukuran jendela agar rasio koordinat canvas tetap presisi pada 360x640 piksel
        frame.setResizable(false);

        // Alokasikan pengelola audio native untuk memuat dan memutar klip suara
        SoundManager soundManager = new SoundManager();
        // Inisialisasi komponen View (container CardLayout untuk Menu dan GamePanel)
        View view = new View();
        // Inisialisasi pengendali logika permainan yang mengelola siklus timer dan status entitas
        Logic logic = new Logic(view, soundManager);

        // Kaitkan pengendali logika ke komponen View untuk penanganan event tombol dan keyboard
        view.init(logic);
        // Daftarkan referensi balik View ke Logic agar pembaruan model dapat meminta kanvas menggambar ulang
        logic.setView(view);

        // Masukkan panel utama antarmuka ke dalam container JFrame
        frame.add(view);
        // Sesuaikan ukuran frame secara otomatis agar dimensi area konten tepat 360x640 piksel
        frame.pack();
        // Posisikan jendela tepat di tengah layar monitor saat pertama kali ditampilkan
        frame.setLocationRelativeTo(null);
        // Render dan tampilkan jendela permainan ke layar monitor
        frame.setVisible(true);
    }
}
