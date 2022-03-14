public class IntraAnalysis {

    int g1;

    IntraAnalysis() {
        g1 = 1;
    }

    void IntraAnalysis() {
        obj2 = new Obj();
    }

    public int methodA() {
        Obj obj = new Obj();
        obj.f2 = null;
        c = obj.f1;
        obj.f1 = 2;

        int a = obj.f1;
        int b = obj.f2;
        return c;
    }

    class Obj {
        int f1 = 1;
        int f2 = 2;
    }
}