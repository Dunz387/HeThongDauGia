import java.util.ArrayList;

abstract class HangHoa {
    protected String maHang;
    protected String tenHang;

    public HangHoa(String maHang, String tenHang) {
        this.maHang = maHang;
        this.tenHang = tenHang;
    }

    public String getMaHang() {
        return maHang;
    }

    public String getTenHang() {
        return tenHang;
    }

    public abstract void hienThiThongTin();
}

class ThucPham extends HangHoa {
    private String hanSuDung;

    public ThucPham(String maHang, String tenHang, String hanSuDung) {
        super(maHang, tenHang);
        this.hanSuDung = hanSuDung;
    }

    @Override
    public void hienThiThongTin() {
        System.out.println(tenHang + " - Han su dung: " + hanSuDung);
    }
}

class DienTu extends HangHoa {
    private int thoiGianBaoHanh;

    public DienTu(String maHang, String tenHang, int thoiGianBaoHanh) {
        super(maHang, tenHang);
        this.thoiGianBaoHanh = thoiGianBaoHanh;
    }

    @Override
    public void hienThiThongTin() {
        System.out.println(tenHang + " - Bao hanh: " + thoiGianBaoHanh + " thang");
    }
}

class Kho<T extends HangHoa> {
    private ArrayList<T> danhSach = new ArrayList<>();

    public void nhapKho(T hang) {
        danhSach.add(hang);
    }

    public void xuatKho(String maHang) {
        danhSach.removeIf(h -> h.getMaHang().equals(maHang));
    }

    public void kiemKe() {
        for (T hang : danhSach) {
            hang.hienThiThongTin();
        }
    }
}

public class Main09 {
    public static void main(String[] args) {
        Kho<ThucPham> khoTP = new Kho<>();
        khoTP.nhapKho(new ThucPham("TP01", "Sua", "01/01/2026"));
        khoTP.nhapKho(new ThucPham("TP02", "Banh", "10/10/2025"));
        System.out.println("Kho thuc pham:");
        khoTP.kiemKe();

        Kho<DienTu> khoDT = new Kho<>();
        khoDT.nhapKho(new DienTu("DT01", "Tivi", 24));
        khoDT.nhapKho(new DienTu("DT02", "Laptop", 12));

        System.out.println("\nKho dien tu:");
        khoDT.kiemKe();
    }
}