public class Cell {
    private boolean mayinMi;
    private boolean acildiMi;
    private boolean isaretlendi;
    private int komsuMayinSayisi;

    public Cell() {
        this.mayinMi = false;
        this.acildiMi = false;
        this.isaretlendi = false;
        this.komsuMayinSayisi = 0;
    }

    public boolean isMayinMi() {
        return mayinMi;
    }

    public boolean isAcildiMi() {
        return acildiMi;
    }

    public boolean isIsaretlendi() {
        return isaretlendi;
    }

    public int getKomsuMayinSayisi() {
        return komsuMayinSayisi;
    }

    public void setMayin(boolean mayinMi) {
        this.mayinMi = mayinMi;
    }

    public void setKomsuMayinSayisi(int sayi) {
        this.komsuMayinSayisi = sayi;
    }

    public void ac() {
        if (!isaretlendi) {
            this.acildiMi = true;
        }
    }

    public void isaretiBegistir() {
        if (!acildiMi) {
            this.isaretlendi = !this.isaretlendi;
        }
    }

    public boolean bosHucreMi() {
        return komsuMayinSayisi == 0 && !mayinMi;
    }

    public boolean isMine() { return mayinMi; }
    public boolean isRevealed() { return acildiMi; }
    public boolean isFlagged() { return isaretlendi; }
    public int getNeighborMines() { return komsuMayinSayisi; }
    public void reveal() { ac(); }
    public void toggleFlag() { isaretiBegistir(); }
    public boolean isEmpty() { return bosHucreMi(); }
}
