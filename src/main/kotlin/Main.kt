val server : Server = Server()
var running : Boolean = false

/**
 * Main
 *
 */
fun main() {
    running = true
    do {
        displayMenu()
        print("Enter number \n>>")
        val input = readLine() ?: "0"
        menuHandler(input)

    } while(running)
}

/**
 * Display menu
 *
 */
private fun displayMenu() {
    println("\nInstructions:\n\t" +
            "Input number that corresponds to wanted action.\n\t" +
            "Incorrect input/syntax results in reprint of menu.\n\n" +
            "Menu\n\t" +
            "0. Instructions and menu\n\t" +
            "1. Parse AVLs (verbose)\n\t" +
            "2. Parse AVLs \n\t" +
            "3. Send getinfo GPRS command \n" +
            "\t\t-1. Exit\n\t")
}

/**
 * Menu handler. Function [readNumber] is used to get the number that represents the choice of the user.
 * Based on this number, the when-case will do an action.
 *
 * @param input The input from the user.
 */
private fun menuHandler(input: String) {
    val choice = readNumber(input)
    when(choice) {
        -1 -> running = false
        0 -> return
        1 -> server.fmb003CommunicationHandler(parseAvl = true, verboseParsing = true)
        2 -> server.fmb003CommunicationHandler(parseAvl = true)
        3 -> server.fmb003CommunicationHandler("getinfo", parseAvl = true, verboseParsing = true)
    }
}

/**
 * Handler function that does all the necessary checks on the inputted value from the user.
 *
 * @param input The inputted value
 * @return      A number representing the choice of the user, in terms of the menu options.
 */
private fun readNumber(input : String) : Int {
    if(!checkNumber(input)) return 0
    val inputNumber = input.toInt()
    if(checkExit(inputNumber)) return -1
    if(!checkRange(inputNumber)) return 0
    return inputNumber
}

/**
 * Checks the validity of the number, whether it is Int or null.
 *
 * @param input The input from the terminal
 * @return      Boolean value representing the validity of the number.
 */
private fun checkNumber(input : String) : Boolean {
    return input.toIntOrNull() != null
}

/**
 * Checks whether the user wants to exit program.
 *
 * @param input The input
 * @return      Whether the user wants to exit, true if user wants to exit, false if not.
 */
private fun checkExit(input : Int) : Boolean {
    return input == -1
}

/**
 * Checks the range of inputted number.
 *
 * @param input     The input.
 * @return          Whether it is valid.
 */
private fun checkRange(input : Int) : Boolean {
    return input in 1..3
}