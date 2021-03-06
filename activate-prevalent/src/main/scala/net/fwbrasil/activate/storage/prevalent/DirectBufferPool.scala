package net.fwbrasil.activate.storage.prevalent

import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue
import scala.collection.JavaConversions.collectionAsScalaIterable
import java.nio.ByteOrder

class DirectBufferPool(bufferSize: Int, bufferPoolSize: Int) {

    private val pool = new LinkedBlockingQueue[ByteBuffer](bufferPoolSize)
    
    for(i <- 0 until bufferPoolSize)
        pool.add(ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder))

    def pop =
        pool.take
    
    def push(buffer: ByteBuffer) =
        pool.offer(buffer)
        
    def destroy = 
        pool.foreach(byteBufferCleaner.cleanDirect)
    
}