public class IntraAnalysis {


    public Obj methodA() {
       Obj obj = new Obj();
       Obj obj2 = new Obj();
       int a = obj2.f1;
       return obj;
    }

    class Obj {
        int f1 = 1;
        int f2 = 2;
    }
}