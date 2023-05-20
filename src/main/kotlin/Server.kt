import avl.parseAvlCodec8eHandler
import gprs.encodeCodec12Request
import java.io.*
import java.net.ServerSocket
import java.nio.ByteBuffer

class Server {

    private val port = 5000
    private val serverSocket = ServerSocket(port)

    /**
     * Fmb003communication handler. Uses ServerSocket to establish connection with FMB003.
     *
     * @param command           The command to be passed to the FMB003.
     * @param parseAvl          Whether to parse the AVL.
     * @param verboseParsing    Whether the parsing should be verbose.
     */
    fun fmb003CommunicationHandler(command : String = "", parseAvl: Boolean = false, verboseParsing: Boolean = false) {
        //Waiting for client/device to connect
        println("Server listening on port $port")
        println("Waiting for client to connect ...")
        val clientSocket = serverSocket.accept() // Wait for client connection
        println("Client connected: ${clientSocket.inetAddress.hostAddress}")

        // Receiving IMEI number
        val inputStream = clientSocket.getInputStream()
        val imeiBuffer = ByteArray(1024)
        val bytesReadImei = inputStream.read(imeiBuffer)
        val imei = convertHex(imeiBuffer, bytesReadImei)
        println("IMEI: $imei")

        // Sending ACK
        println("Sending ACK to FMB003 ...")
        val outputStream = clientSocket.getOutputStream()
        val ack = byteArrayOf(0x01)
        outputStream.write(ack)

        // Handling and parsing AVLs
        println("AVLs ...")
        handleAVLs(inputStream, outputStream, parseAvl = parseAvl, verboseParsing = verboseParsing)

        // If there is a command, send the command. If not, exit.
        if (command.isEmpty()) return
        val request = encodeCodec12Request(command)
        println("Sending request with command $command to FMB003 ...")
        outputStream.write(request)

        println("Fetching input stream from FMB003 ...")

        val responseBuffer = ByteArray(8192)
        var bytesRead = 0
        while(bytesRead < 1) bytesRead = inputStream.read(responseBuffer)
        val gprsResponse = convertHex(responseBuffer, bytesRead)

        println("Received data from device ... \n\t bytes read: $bytesRead \n\t")
        println("As hex:\n$gprsResponse\n\n")

        clientSocket.close()
        serverSocket.close()
    }

    /**
     * Handle all cached AVLs
     *
     * @param inputStream       The input stream from the client.
     * @param outputStream      The output stream to the client.
     * @param parseAvl          Whether to parse the AVL.
     * @param verboseParsing    Whether the parsing should be verbose.
     * @return                  True if all AVLs has been read.
     */
    private fun handleAVLs(inputStream : InputStream, outputStream : OutputStream, parseAvl: Boolean = false,
                           verboseParsing: Boolean = false) : Boolean {
        var avlIndex = 1
        while (true) {
            println("Fetching AVL ${avlIndex} from FMB003 ...")

            val buffer = ByteArray(8192)

            val bytesRead = inputStream.read(buffer)

            if (bytesRead == -1) return true
            val avl = convertHex(buffer, bytesRead)

            val size = Integer.parseInt(avl.substring(18, 20), 16)
            val response = ByteBuffer.allocate(4).putInt(size).array()
            outputStream.write(response)
            if (parseAvl) {
                val avlParsed = parseAvlCodec8eHandler(avl, verbose = verboseParsing)
                if (!verboseParsing) println(avlParsed)
            }
            else println("As hex:\n$avl\n\n")
            avlIndex++
        }
    }


    /**
     * Converts hexadecimal string from bytearray to ASCII string.
     *
     * @param buffer        The byte array.
     * @param bytesRead     The number of bytes.
     * @return              A string with the hexadecimal values of teh bytearray converted to ASCII.
     */
    fun convertHex(buffer: ByteArray, bytesRead: Int) : String {
        val sb = StringBuilder()
        for(i in 0 until bytesRead ) {
            val byteValue = buffer[i]
            sb.append(String.format("%02X", byteValue))
        }
        return sb.toString()
    }
}