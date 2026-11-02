import java.awt.Image;
import java.awt.Rectangle;

public class Pipe {
    // Lebar fisik rintangan pipa dalam piksel sesuai proporsi tampilan grafis
    public static final int WIDTH = 64;
    // Tinggi fisik rintangan pipa dibuat melampaui separuh layar untuk menutupi batas atas/bawah
    public static final int HEIGHT = 512;
    // Kecepatan horizontal translasi pipa bergerak ke arah kiri layar (arah negatif sumbu X)
    public static final int VELOCITY_X = -2;

    // Koordinat posisi horizontal pipa saat ini di layar
    private int x;
    // Koordinat posisi vertikal pipa saat ini di layar
    private int y;
    // Lebar pipa yang tidak berubah sepanjang siklus hidup objek
    private final int width;
    // Tinggi pipa yang tidak berubah sepanjang siklus hidup objek
    private final int height;
    // Objek grafis yang menyimpan gambar sprite pipa (atas atau bawah)
    private Image image;
    // Penanda status apakah rintangan pipa ini sudah berhasil dilewati pemain
    private boolean passed;
    // Flag penentu tipe pipa (true = pipa atas, false = pipa bawah) untuk diferensiasi scoring
    private final boolean upper;

    public Pipe(int x, int y, Image image, boolean upper) {
        // Tetapkan posisi X awal saat pipa dimunculkan di tepi kanan layar
        this.x = x;
        // Tetapkan posisi Y awal berdasarkan hasil kalkulasi celah vertikal
        this.y = y;
        // Gunakan lebar baku dari konstanta kelas
        this.width = WIDTH;
        // Gunakan tinggi baku dari konstanta kelas
        this.height = HEIGHT;
        // Pasang aset gambar sprite yang sesuai dengan posisi pipa
        this.image = image;
        // Inisialisasi status kelolosan awal menjadi belum dilewati
        this.passed = false;
        // Simpan kategori posisi vertikal pipa dalam pasangan rintangan
        this.upper = upper;
    }

    public void move() {
        // Geser posisi horizontal pipa ke kiri berdasarkan konstanta kecepatan per frame
        x += VELOCITY_X;
    }

    public Rectangle getBounds() {
        // Buat kotak pembatas (AABB) untuk evaluasi benturan dengan bounding box pemain
        return new Rectangle(x, y, width, height);
    }

    public int getX() {
        // Dapatkan koordinat horizontal terkini dari pipa
        return x;
    }

    public void setX(int x) {
        // Perbarui koordinat horizontal pipa jika dibutuhkan penyesuaian posisi
        this.x = x;
    }

    public int getY() {
        // Dapatkan koordinat vertikal terkini dari pipa
        return y;
    }

    public void setY(int y) {
        // Perbarui koordinat vertikal pipa jika dibutuhkan penyesuaian posisi
        this.y = y;
    }

    public int getWidth() {
        // Dapatkan ukuran lebar pipa untuk perhitungan collision dan deteksi skor
        return width;
    }

    public int getHeight() {
        // Dapatkan ukuran tinggi pipa untuk perhitungan celah lintasan
        return height;
    }

    public Image getImage() {
        // Akses aset gambar pipa yang akan dirender pada kanvas tampilan
        return image;
    }

    public void setImage(Image image) {
        // Perbarui gambar sprite pipa jika terjadi pergantian tema atau reload aset
        this.image = image;
    }

    public boolean isPassed() {
        // Periksa apakah pipa ini sudah pernah dihitung dalam penambahan skor pemain
        return passed;
    }

    public void setPassed(boolean passed) {
        // Tandai status kelolosan agar skor tidak bertambah secara berulang pada pipa yang sama
        this.passed = passed;
    }

    public boolean isUpper() {
        // Identifikasi apakah pipa ini adalah pipa bagian atas pasangan rintangan
        return upper;
    }
}
