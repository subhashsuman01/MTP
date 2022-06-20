import java.util.*;

public class IntraAnalysis {
    public static Obj fld;
    class Obj {
        int f1 = 1;
        int f2 = 2;
        public Obj(){
            this.f1 = 2;
        }
    }
    public void methodA(Obj obj){
    }

    public void methodB(Obj obj){
    }

    public void methodC(){
    }

    public void method() {
        Scanner scan = new Scanner(System.in);
        String name = scan.nextLine();
        Obj obj = new Obj();
        if(name.equals("tom")){
            methodA(obj);
        } else {
            obj.f1 = 2;
        }
    }
}