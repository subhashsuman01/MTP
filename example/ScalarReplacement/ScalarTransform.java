import java.util.*;

public class ScalarTransform {

    public static Obj fld;

    class Obj {
        int f1 = 1;
        int f2 = 2;
    }

    public void methodA(int a){
        // pass
    }

    public void methodB(Obj obj){
        // pass
    }

    public void methodC(){
        // pass
    }

    public Obj method() {
        Obj ref = new Obj();
        Obj ref2 = new Obj();
        ref2 = ref;
        ref.f1 = 4;
        ref.f2 = 5;
        ref.f1 = ref2.f2;
        methodA(ref.f1);
        methodB(ref2);
        methodC();
        return ref;
//        Scanner scan = new Scanner(System.in);
//        String name = scan.nextLine();
//        Obj obj = new Obj();
//        if(name.equals("Hi")){
//            // escaping in this branch
//            fld = obj;
//            methodA();
//        } else {
//            // not escaping in this branch
//            obj.f1 = 2;
//            methodB();
//        }
//        methodC();
    }
}