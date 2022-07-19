data class Main(var name : String): Test(33) { //
//    var name: String = ""
    var age: Int = 0

//    constructor(name: String, ) : this(name) {
//        this.name = "d";
//    }

//    constructor(name: String, age: Int ) :this(name)  { //super(33)
//        println(name)
//    }

//    init {
//        this.name2 = "seols"
//        this.age2 = 11
//    }

    fun greet(str: String) {
        var a = 10;
        val b = 20;
        println("Hello" + str+ " " + (a+b))

    }

}

fun main(){
    val test = Main("필드 변수 테스트");
    test.name = "필드변수 테스트 setter!";
    println("Hello world "+ test.name+ " " + test.age2+ " " + test.age3);
    test.greet("seol");

}

