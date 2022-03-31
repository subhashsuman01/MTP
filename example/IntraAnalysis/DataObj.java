import java.util.*;

public class IntraAnalysis {
    public static Obj fld;
    class Obj {
        int f1 = 1;
        int f2 = 2;
    }
    public void methodA(Obj obj){
    }

    public void methodB(int a){
    }

    public void methodC(){
    }

    public void method() {
        Scanner scan = new Scanner(System.in);
        String name = scan.nextLine();

        random_num$f1 = init();
        if(name.equals("tom")){
            Obj obj = new Obj();
            fld = obj;
            methodA(obj);
        } else {
            random_num$f1 = init();
            int a = f1;
            methodB(a);
        }
        methodC();
    }
}
