import java.util.*;

public class ScalarTransform {

    public static Obj fld;

    class Obj {
        int f1 = 1;
        int f2 = 2;
    }

    public void methodA(){
        // todo
    }

    public void methodB(){
        // todo
    }

    public void methodC(){
        // todo
    }

    public void method() {
        Scanner scan = new Scanner(System.in);
        String name = scan.nextLine();
        Obj obj = new Obj();
        if(name.equals("Hi")){
            // escaping in this branch
            fld = obj;
            methodA();
        } else {
            // not escaping in this branch
            methodB();
        }
        methodC();
    }
}