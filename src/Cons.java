public class Cons {
    enum keyRequest{
        //Both server and client
        Info, //gui thong bao - in ra console(giam sat, fix bug, ...)
        Notify, //quan ly thong bao - viet len UI cho nguoi dung xem

        //server send
        GetAllFile, //thong bao voi client muon xem tat ca cac file trong thu muc duoc quan ly
        GetFolder, //thong bao voi client muon xem tat ca cac folder trong thu muc duoc phep quan ly
        ChosePath, //thong bao voi client folder duoc chon de quan ly

        //client send
        PathFolderClient, //gui cac folder tra loi cho GetAllFile, GetFolder
        Observer //thong bao voi server da nhan duoc thong tin thu muc duoc chon de quan ly
    }

    public static final String Sign = "|";
}
