import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MinesweeperApp extends Application {

    // ── Oyun sabitleri ────────────────────────────────────────────────────────
    private static final int SATIR_SAYISI  = 10;
    private static final int SUTUN_SAYISI  = 10;
    private static final int MAYIN_SAYISI  = 11;

    // ── Oyun durumu ───────────────────────────────────────────────────────────
    private Board tahta;
    private Button[][] dugmeler;
    private int yerlestirilenIsaret;
    private int gecenSaniye;

    // ── Arayüz bileşenleri ────────────────────────────────────────────────────
    private Label maynSayaciEtiketi;
    private Label zamanlayiciEtiketi;
    private Label durumEtiketi;
    private Button sifirlaBtn;
    private Button temaBtn;
    private Timeline zamanlayici;
    private boolean karanlikTema = true;
    private BorderPane kokDuzen;
    private GridPane izgaraDuzen;
    private Scene sahne;

    // ── Karanlık Tema ─────────────────────────────────────────────────────────
    private static final String KT_ARKAPLAN = "#1e1e2e";
    private static final String KT_ACILMAMIS = "#454158";
    private static final String KT_ACILMIS = "#11111b";
    private static final String KT_ISARETLI = "#524f6e";
    private static final String KT_MAYIN = "#f38ba8";
    private static final String KT_CERCEVE = "#6c7086";
    private static final String KT_YAZI = "#cdd6f4";
    private static final String KT_YAZI_SOLUK = "#6c7086";
    private static final String KT_UST_BAR = "#181825";

    // ── Aydınlık Tema ─────────────────────────────────────────────────────────
    private static final String AT_ARKAPLAN = "#e8eaf0";
    private static final String AT_ACILMAMIS = "#c0c8d8";
    private static final String AT_ACILMIS = "#f4f4f4";
    private static final String AT_ISARETLI = "#b0b8cc";
    private static final String AT_MAYIN = "#e57373";
    private static final String AT_CERCEVE = "#9aa0b0";
    private static final String AT_YAZI = "#1e1e2e";
    private static final String AT_YAZI_SOLUK = "#9aa0b0";
    private static final String AT_UST_BAR = "#d0d4de";

    // ── Sayı renkleri ─────────────────────────────────────────────────────────
    private static final String[] KARANLIK_SAYI_RENKLERI = {
        "", "#89b4fa", "#a6e3a1", "#f38ba8",
        "#74c7ec", "#fab387", "#89dceb", "#b4befe", "#cdd6f4"
    };
    private static final String[] AYDINLIK_SAYI_RENKLERI = {
        "", "#1565c0", "#2e7d32", "#c62828",
        "#0277bd", "#e65100", "#00838f", "#6a1b9a", "#37474f"
    };

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void start(Stage sahne) {
        kokDuzen = new BorderPane();
        kokDuzen.setPadding(new Insets(16));

        ustBariOlustur();
        izgarayiOlustur();
        temayiUygula();
        zamanlayiciBaslat();
        arayuzuGuncelle();

        this.sahne = new Scene(kokDuzen, 600, 680);

        // Pencere boyutu değiştikçe hücre boyutlarını yeniden hesapla
        this.sahne.widthProperty().addListener((gozlemci, eski, yeni) -> hucreBoyutlariniGuncelle());
        this.sahne.heightProperty().addListener((gozlemci, eski, yeni) -> hucreBoyutlariniGuncelle());

        sahne.setScene(this.sahne);
        sahne.setTitle("Mayın Tarlası");
        sahne.setResizable(true);
        sahne.show();

        hucreBoyutlariniGuncelle();
    }

    // ── Hücre boyutlarının dinamik hesaplanması ───────────────────────────────

    private void hucreBoyutlariniGuncelle() {
        if (dugmeler == null) return;

        // Üst bar (~80px), kenarlar (~32px) ve hücreler arası boşluklar için pay bırak
        double kullanilabilirGenislik  = sahne.getWidth()  - 52;
        double kullanilabilirYukseklik = sahne.getHeight() - 120;

        double hucrreGenisligi  = Math.floor(kullanilabilirGenislik  / SUTUN_SAYISI);
        double hucrreYuksekligi = Math.floor(kullanilabilirYukseklik / SATIR_SAYISI);
        double hucreBoyutu = Math.max(32, Math.min(hucrreGenisligi, hucrreYuksekligi));

        // Hücre büyüdükçe yazı tipi de orantılı büyüsün
        double yaziBoyutu = Math.max(10, hucreBoyutu * 0.28);

        for (int s = 0; s < SATIR_SAYISI; s++) {
            for (int u = 0; u < SUTUN_SAYISI; u++) {
                Button btn  = dugmeler[s][u];
                btn.setPrefSize(hucreBoyutu, hucreBoyutu);
                btn.setMinSize(hucreBoyutu, hucreBoyutu);
                btn.setMaxSize(hucreBoyutu, hucreBoyutu);

                // Açılmış sayı hücrelerinde yazı boyutunu güncelle
                Cell hucre = tahta.getHucre(s, u);
                if (hucre.isAcildiMi() && !hucre.isMayinMi()) {
                    String[] sayiRenkleri = karanlikTema ? KARANLIK_SAYI_RENKLERI : AYDINLIK_SAYI_RENKLERI;
                    int komsular = hucre.getKomsuMayinSayisi();
                    btn.setStyle(acilmisHucreTarzi(komsular, sayiRenkleri, yaziBoyutu));
                } else if (!hucre.isAcildiMi() && !hucre.isIsaretlendi()) {
                    btn.setStyle(acilmamisHucreTarzi());
                }
            }
        }
    }

    // ── Üst Bar ───────────────────────────────────────────────────────────────

    private void ustBariOlustur() {
        maynSayaciEtiketi = new Label("💣 " + MAYIN_SAYISI);
        maynSayaciEtiketi.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        sifirlaBtn = new Button("😊");
        sifirlaBtn.setStyle(
            "-fx-font-size: 18px; -fx-padding: 4 12 4 12;" +
            "-fx-cursor: hand; -fx-border-radius: 6; -fx-background-radius: 6;"
        );
        sifirlaBtn.setOnAction(olay -> oyunuSifirla());

        zamanlayiciEtiketi = new Label("⏱ 0s");
        zamanlayiciEtiketi.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        durumEtiketi = new Label("");
        durumEtiketi.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        temaBtn = new Button(karanlikTema ? "☀ Aydınlık" : "★ Karanlık");
        temaBtn.setStyle(
            "-fx-font-size: 13px; -fx-padding: 4 10 4 10;" +
            "-fx-cursor: hand; -fx-border-radius: 6; -fx-background-radius: 6;"
        );
        temaBtn.setOnAction(olay -> temaDegistir());

        HBox ustBar = new HBox(maynSayaciEtiketi, sifirlaBtn, zamanlayiciEtiketi, durumEtiketi, temaBtn);
        ustBar.setAlignment(Pos.CENTER);
        ustBar.setSpacing(16);
        ustBar.setPadding(new Insets(0, 0, 12, 0));
        kokDuzen.setTop(ustBar);
    }

    // ── Izgara ────────────────────────────────────────────────────────────────

    private void izgarayiOlustur() {
        izgaraDuzen = new GridPane();
        izgaraDuzen.setHgap(2);
        izgaraDuzen.setVgap(2);
        izgaraDuzen.setAlignment(Pos.CENTER);

        GridPane.setHgrow(izgaraDuzen, Priority.ALWAYS);
        GridPane.setVgrow(izgaraDuzen, Priority.ALWAYS);

        dugmeler        = new Button[SATIR_SAYISI][SUTUN_SAYISI];
        tahta           = new Board(SATIR_SAYISI, SUTUN_SAYISI, MAYIN_SAYISI);
        yerlestirilenIsaret = 0;

        for (int s = 0; s < SATIR_SAYISI; s++) {
            for (int u = 0; u < SUTUN_SAYISI; u++) {
                Button btn = new Button();
                btn.setPrefSize(52, 52);
                btn.setMinSize(32, 32);

                int satirNo = s, sutunNo = u;
                btn.setOnMouseClicked(olay -> {
                    if (tahta.isOyunBitti() || tahta.kazanildiMi()) return;

                    if (olay.getButton() == MouseButton.PRIMARY) {
                        // Sol tık: hücreyi aç
                        tahta.ac(satirNo, sutunNo);
                    } else if (olay.getButton() == MouseButton.SECONDARY) {
                        // Sağ tık: bayrak koy / kaldır
                        boolean isaretliydi = tahta.getHucre(satirNo, sutunNo).isIsaretlendi();
                        tahta.getHucre(satirNo, sutunNo).isaretiBegistir();
                        yerlestirilenIsaret += isaretliydi ? -1 : 1;
                        maynSayaciEtiketi.setText("💣 " + (MAYIN_SAYISI - yerlestirilenIsaret));
                    }

                    arayuzuGuncelle();
                    hucreBoyutlariniGuncelle();
                });

                dugmeler[s][u] = btn;
                izgaraDuzen.add(btn, u, s);
            }
        }

        StackPane merkezPanel = new StackPane(izgaraDuzen);
        merkezPanel.setAlignment(Pos.CENTER);
        VBox.setVgrow(merkezPanel, Priority.ALWAYS);
        kokDuzen.setCenter(merkezPanel);
    }

    // ── Tema ──────────────────────────────────────────────────────────────────

    private void temaDegistir() {
        karanlikTema = !karanlikTema;
        temaBtn.setText(karanlikTema ? "☀ Aydınlık" : "★ Karanlık");
        temayiUygula();
        arayuzuGuncelle();
        hucreBoyutlariniGuncelle();
    }

    private void temayiUygula() {
        String arkaplan  = karanlikTema ? KT_ARKAPLAN  : AT_ARKAPLAN;
        String ustBarRng = karanlikTema ? KT_UST_BAR   : AT_UST_BAR;
        String yaziRengi = karanlikTema ? KT_YAZI      : AT_YAZI;
        String cerceveRg = karanlikTema ? KT_CERCEVE   : AT_CERCEVE;

        kokDuzen.setStyle("-fx-background-color: " + arkaplan + ";");

        if (kokDuzen.getTop() instanceof HBox ustBar) {
            ustBar.setStyle(
                "-fx-background-color: " + ustBarRng + ";" +
                "-fx-background-radius: 8; -fx-padding: 8 12 8 12;"
            );
            ustBar.getChildren().forEach(dugum -> {
                if (dugum instanceof Label etiket) {
                    etiket.setStyle(etiket.getStyle() + "-fx-text-fill: " + yaziRengi + ";");
                } else if (dugum instanceof Button btn) {
                    btn.setStyle(btn.getStyle() +
                        "-fx-background-color: " + (karanlikTema ? "#313244" : "#c8cdd8") + ";" +
                        "-fx-text-fill: " + yaziRengi + ";" +
                        "-fx-border-color: " + cerceveRg + ";"
                    );
                }
            });
        }

        if (izgaraDuzen != null) {
            izgaraDuzen.setStyle("-fx-background-color: " + cerceveRg + "; -fx-padding: 2;");
        }

        // Oyun bitmişse durum etiketi rengini güncelle
        if (tahta != null && tahta.kazanildiMi()) {
            durumEtiketi.setStyle(
                "-fx-font-size: 15px; -fx-font-weight: bold;" +
                "-fx-text-fill: " + (karanlikTema ? "#a6e3a1" : "#2e7d32") + ";"
            );
        } else if (tahta != null && tahta.isOyunBitti()) {
            durumEtiketi.setStyle(
                "-fx-font-size: 15px; -fx-font-weight: bold;" +
                "-fx-text-fill: " + (karanlikTema ? "#f38ba8" : "#c62828") + ";"
            );
        }
    }

    // ── Zamanlayıcı ───────────────────────────────────────────────────────────

    private void zamanlayiciBaslat() {
        gecenSaniye = 0;
        if (zamanlayici != null) zamanlayici.stop();
        zamanlayici = new Timeline(new KeyFrame(Duration.seconds(1), olay -> {
            gecenSaniye++;
            zamanlayiciEtiketi.setText("⏱ " + gecenSaniye + "s");
        }));
        zamanlayici.setCycleCount(Animation.INDEFINITE);
        zamanlayici.play();
    }

    // ── Oyunu sıfırla ─────────────────────────────────────────────────────────

    private void oyunuSifirla() {
        if (zamanlayici != null) zamanlayici.stop();
        yerlestirilenIsaret = 0;
        maynSayaciEtiketi.setText("💣 " + MAYIN_SAYISI);
        zamanlayiciEtiketi.setText("⏱ 0s");
        sifirlaBtn.setText("😊");
        durumEtiketi.setText("");
        izgarayiOlustur();
        temayiUygula();
        zamanlayiciBaslat();
        arayuzuGuncelle();
        hucreBoyutlariniGuncelle();
    }

    // ── Arayüz güncelleme ─────────────────────────────────────────────────────

    private void arayuzuGuncelle() {
        if (tahta == null || dugmeler == null) return;
        String[] sayiRenkleri = karanlikTema ? KARANLIK_SAYI_RENKLERI : AYDINLIK_SAYI_RENKLERI;

        for (int s = 0; s < SATIR_SAYISI; s++) {
            for (int u = 0; u < SUTUN_SAYISI; u++) {
                Cell hucre = tahta.getHucre(s, u);
                Button btn = dugmeler[s][u];

                if (hucre.isAcildiMi()) {
                    if (hucre.isMayinMi()) {
                        btn.setText("X");
                        btn.setStyle(mayinHucreTarzi());
                    } else {
                        int komsular = hucre.getKomsuMayinSayisi();
                        btn.setText(komsular == 0 ? "" : String.valueOf(komsular));
                        btn.setStyle(acilmisHucreTarzi(komsular, sayiRenkleri));
                    }
                    btn.setDisable(true);

                } else if (hucre.isIsaretlendi()) {
                    btn.setText("F");
                    btn.setStyle(isaretliHucreTarzi());
                    btn.setDisable(false);

                } else {
                    btn.setText("");
                    btn.setStyle(acilmamisHucreTarzi());
                    btn.setDisable(false);
                }
            }
        }

        if (tahta.isOyunBitti()) {
            zamanlayici.stop();
            sifirlaBtn.setText("😵");
            durumEtiketi.setText("✖ Oyun Bitti!");
            durumEtiketi.setStyle(
                "-fx-font-size: 15px; -fx-font-weight: bold;" +
                "-fx-text-fill: " + (karanlikTema ? "#f38ba8" : "#c62828") + ";"
            );
        } else if (tahta.kazanildiMi()) {
            zamanlayici.stop();
            sifirlaBtn.setText("😎");
            durumEtiketi.setText("★ Kazandınız!");
            durumEtiketi.setStyle(
                "-fx-font-size: 15px; -fx-font-weight: bold;" +
                "-fx-text-fill: " + (karanlikTema ? "#a6e3a1" : "#2e7d32") + ";"
            );
        }
    }

    // ── Hücre görsel stilleri ─────────────────────────────────────────────────

    private String acilmamisHucreTarzi() {
        String arkaplan = karanlikTema ? KT_ACILMAMIS : AT_ACILMAMIS;
        String cerceve = karanlikTema ? KT_CERCEVE   : AT_CERCEVE;
        return "-fx-background-color: " + arkaplan + ";" +
               "-fx-border-color: " + cerceve + ";" +
               "-fx-border-width: 1; -fx-background-radius: 3;" +
               "-fx-border-radius: 3; -fx-padding: 0; -fx-cursor: hand;";
    }

    /** Varsayılan yazı boyutu (14px) ile açılmış hücre tarzi. */
    private String acilmisHucreTarzi(int komsular, String[] sayiRenkleri) {
        return acilmisHucreTarzi(komsular, sayiRenkleri, 14);
    }

    private String acilmisHucreTarzi(int komsular, String[] sayiRenkleri, double yaziBoyutu) {
        String arkaplan = karanlikTema ? KT_ACILMIS    : AT_ACILMIS;
        String cerceve = karanlikTema ? KT_CERCEVE    : AT_CERCEVE;
        String yaziRengi = (komsular > 0 && komsular <= 8)
                           ? sayiRenkleri[komsular]
                           : (karanlikTema ? KT_YAZI_SOLUK : AT_YAZI_SOLUK);
        return "-fx-background-color: " + arkaplan + ";" +
               "-fx-border-color: " + cerceve + ";" +
               "-fx-border-width: 1; -fx-background-radius: 3;" +
               "-fx-border-radius: 3; -fx-padding: 0;" +
               "-fx-text-fill: " + yaziRengi + ";" +
               "-fx-font-weight: bold; -fx-font-size: " + yaziBoyutu + "px;";
    }

    private String isaretliHucreTarzi() {
        String arkaplan = karanlikTema ? KT_ISARETLI : AT_ISARETLI;
        String cerceve = karanlikTema ? KT_CERCEVE  : AT_CERCEVE;
        String yaziRengi = karanlikTema ? "#f38ba8"   : "#c62828";
        return "-fx-background-color: " + arkaplan + ";" +
               "-fx-border-color: " + cerceve + ";" +
               "-fx-border-width: 1; -fx-background-radius: 3;" +
               "-fx-border-radius: 3; -fx-padding: 0;" +
               "-fx-text-fill: " + yaziRengi + ";" +
               "-fx-font-weight: bold; -fx-font-size: 14px; -fx-cursor: hand;";
    }

    private String mayinHucreTarzi() {
        String arkaplan = karanlikTema ? KT_MAYIN    : AT_MAYIN;
        String yaziRengi = karanlikTema ? "#1e1e2e"   : "#ffffff";
        return "-fx-background-color: " + arkaplan + ";" +
               "-fx-border-width: 1; -fx-background-radius: 3;" +
               "-fx-border-radius: 3; -fx-padding: 0;" +
               "-fx-text-fill: " + yaziRengi + ";" +
               "-fx-font-weight: bold; -fx-font-size: 14px;";
    }

    public static void main(String[] args) {
        launch();
    }
}
