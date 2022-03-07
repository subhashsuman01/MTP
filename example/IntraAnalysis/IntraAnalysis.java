public class IntraAnalysis{

    int globalField;
    static int global;

    class Obj{
        int f1=1;
        int f2=2;
    }

    public void methodA(){
        int a;
        a = 1;
        int b=1;
        a = b;
        global = a;
        int arr[] = new int[10];
        arr[0] = 4;
        Obj obj = new Obj();
        obj.f1 = 1;
    }
}