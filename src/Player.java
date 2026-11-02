import java.awt.Image;
import java.awt.Rectangle;

public class Player {
    // Koordinat horizontal awal: diletakkan di tengah kanvas (360 / 2) untuk memberi ruang pandang pemain
    public static final int INITIAL_X = 180;
    // Koordinat vertikal awal: diletakkan di tengah ketinggian kanvas (640 / 2) agar posisi awal seimbang
    public static final int INITIAL_Y = 320;
    // Lebar bounding box burung dalam piksel sesuai rasio sprite grafis
    public static final int WIDTH = 34;
    // Tinggi bounding box burung dalam piksel
    public static final int HEIGHT = 24;
    // Konstanta percepatan gravitasi yang ditambahkan ke kecepatan vertikal setiap frame
    public static final int GRAVITY = 1;
    // Nilai kecepatan impuls ke atas (koordinat Swing makin ke atas bernilai makin negatif)
    public static final int JUMP_VELOCITY = -10;

    // Koordinat posisi horizontal terkini pada kanvas
    private int x;
    // Koordinat posisi vertikal terkini pada kanvas
    private int y;
    // Dimensi lebar objek yang bersifat tetap sepanjang siklus permainan
    private final int width;
    // Dimensi tinggi objek yang bersifat tetap sepanjang siklus permainan
    private final int height;
    // Kecepatan gerak vertikal yang dipengaruhi oleh gravitasi dan aksi loncat
    private int velocityY;
    // Objek gambar grafis sprite burung untuk digambar pada kanvas antarmuka
    private Image image;

    public Player(Image image) {
        // Tetapkan dimensi fisik berdasarkan konstanta standar permainan
        this.width = WIDTH;
        this.height = HEIGHT;
        // Simpan referensi sprite burung dari pemuat aset View
        this.image = image;
        // Atur posisi dan vektor kecepatan ke nilai awal permainan
        reset();
    }

    public void reset() {
        // Kembalikan koordinat horizontal ke titik awal permainan
        this.x = INITIAL_X;
        // Kembalikan koordinat vertikal ke titik awal permainan
        this.y = INITIAL_Y;
        // Hilangkan akumulasi gaya gravitasi sebelumnya agar burung diam sejenak di awal
        this.velocityY = 0;
    }

    public void move() {
        // Terapkan penambahan percepatan gravitasi ke kecepatan vertikal per frame tick
        velocityY += GRAVITY;
        // Perbarui posisi vertikal berdasarkan akumulasi kecepatan saat ini
        y += velocityY;

        // Batasi pergerakan agar pemain tidak dapat melambung keluar menembus batas atas layar
        if (y < 0) {
            y = 0;
            // Hentikan sisa momentum ke atas ketika menyentuh langit-langit layar
            velocityY = 0;
        }
    }

    public void jump() {
        // Berikan gaya ke atas secara instan menggantikan kecepatan jatuh sebelumnya
        velocityY = JUMP_VELOCITY;
    }

    public Rectangle getBounds() {
        // Bentuk representasi geometri persegi panjang terkini untuk kalkulasi tabrakan AABB
        return new Rectangle(x, y, width, height);
    }

    public int getX() {
        // Akses koordinat posisi horizontal saat ini
        return x;
    }

    public void setX(int x) {
        // Modifikasi koordinat posisi horizontal (digunakan saat penataan posisi)
        this.x = x;
    }

    public int getY() {
        // Akses koordinat posisi vertikal saat ini
        return y;
    }

    public void setY(int y) {
        // Modifikasi koordinat posisi vertikal (digunakan saat penataan posisi)
        this.y = y;
    }

    public int getWidth() {
        // Dapatkan lebar objek burung untuk perhitungan batas dan render
        return width;
    }

    public int getHeight() {
        // Dapatkan tinggi objek burung untuk perhitungan batas dan render
        return height;
    }

    public int getVelocityY() {
        // Dapatkan nilai kecepatan jatuh saat ini untuk keperluan kalkulasi fisika
        return velocityY;
    }

    public void setVelocityY(int velocityY) {
        // Atur ulang kecepatan vertikal secara eksplisit bila diperlukan
        this.velocityY = velocityY;
    }

    public Image getImage() {
        // Ambil referensi aset gambar untuk proses rendering visual pada kanvas
        return image;
    }

    public void setImage(Image image) {
        // Pasang referensi gambar sprite baru ke entitas burung
        this.image = image;
    }
}
