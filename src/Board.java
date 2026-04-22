import java.util.Set;
import java.util.HashSet;
import java.security.SecureRandom;

public class Board {
    private int satirSayisi;
    private int sutunSayisi;
    private int toplamMayin;
    private Cell[][] izgara;
    private boolean oyunBitti = false;
    private boolean ilkTiklama = true;

    public Board(int satirSayisi, int sutunSayisi, int toplamMayin) {
        this.satirSayisi = satirSayisi;
        this.sutunSayisi = sutunSayisi;
        this.toplamMayin = toplamMayin;
        this.izgara = new Cell[satirSayisi][sutunSayisi];
        izgarayiBaslat();
    }

    private void izgarayiBaslat() {
        for (int s = 0; s < satirSayisi; s++) {
            for (int u = 0; u < sutunSayisi; u++) {
                izgara[s][u] = new Cell();
            }
        }
    }

    /** İlk tıklanan hücre ve komşuları güvenli bölge olarak işaretlenir. */
    private Set<Integer> guvenliiBolgeHesapla(int satir, int sutun) {
        Set<Integer> guvenli = new HashSet<>();
        guvenli.add(satir * sutunSayisi + sutun);
        return guvenli;
    }

    private void mayinlariYerlestir(Set<Integer> guvenliiBolge) {
        int yerlestirilen = 0;
        SecureRandom rastgele = new SecureRandom();
        while (yerlestirilen < toplamMayin) {
            int s = rastgele.nextInt(satirSayisi);
            int u = rastgele.nextInt(sutunSayisi);
            if (!izgara[s][u].isMayinMi() && !guvenliiBolge.contains(s * sutunSayisi + u)) {
                izgara[s][u].setMayin(true);
                yerlestirilen++;
            }
        }
    }

    private void komsuMayinlariHesapla() {
        for (int s = 0; s < satirSayisi; s++) {
            for (int u = 0; u < sutunSayisi; u++) {
                if (!izgara[s][u].isMayinMi()) {
                    izgara[s][u].setKomsuMayinSayisi(etrafindakiMayinlariSay(s, u));
                }
            }
        }
    }

    private int etrafindakiMayinlariSay(int satir, int sutun) {
        int sayi = 0;
        for (int ds = -1; ds <= 1; ds++) {
            for (int du = -1; du <= 1; du++) {
                if (ds == 0 && du == 0) continue;
                int ys = satir + ds;
                int yu = sutun + du;
                if (sinirIcindeMi(ys, yu) && izgara[ys][yu].isMayinMi()) {
                    sayi++;
                }
            }
        }
        return sayi;
    }

    public void ac(int satir, int sutun) {
        if (oyunBitti) return;
        if (!sinirIcindeMi(satir, sutun)) return;

        if (ilkTiklama) {
            ilkTiklama = false;
            mayinlariYerlestir(guvenliiBolgeHesapla(satir, sutun));
            komsuMayinlariHesapla();
        }

        Cell hucre = izgara[satir][sutun];
        if (hucre.isAcildiMi() || hucre.isIsaretlendi()) return;
        hucre.ac();

        if (hucre.isMayinMi()) {
            oyunBitti = true;
            tumMayinlariAc();
            return;
        }

        // Boş hücre ise komşuları özyinelemeli aç
        if (hucre.getKomsuMayinSayisi() > 0) return;

        for (int ds = -1; ds <= 1; ds++) {
            for (int du = -1; du <= 1; du++) {
                if (ds == 0 && du == 0) continue;
                ac(satir + ds, sutun + du);
            }
        }
    }

    private void tumMayinlariAc() {
        for (int s = 0; s < satirSayisi; s++) {
            for (int u = 0; u < sutunSayisi; u++) {
                if (izgara[s][u].isMayinMi()) izgara[s][u].ac();
            }
        }
    }

    private boolean sinirIcindeMi(int s, int u) {
        return s >= 0 && s < satirSayisi && u >= 0 && u < sutunSayisi;
    }

    public Cell getHucre(int s, int u) {
        return izgara[s][u];
    }

    public boolean isOyunBitti() {
        return oyunBitti;
    }

    public boolean kazanildiMi() {
        for (int s = 0; s < satirSayisi; s++) {
            for (int u = 0; u < sutunSayisi; u++) {
                Cell hucre = izgara[s][u];
                if (!hucre.isMayinMi() && !hucre.isAcildiMi()) return false;
            }
        }
        return !oyunBitti;
    }
    public void reveal(int satir, int sutun) { ac(satir, sutun); }
    public boolean isGameOver() { return isOyunBitti(); }
    public boolean isWon() { return kazanildiMi(); }
    public Cell getCell(int s, int u) { return getHucre(s, u); }
}
