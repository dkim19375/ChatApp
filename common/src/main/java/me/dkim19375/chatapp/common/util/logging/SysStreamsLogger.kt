/*
 * MIT License
 *
 * Copyright (c) 2023 dkim19375
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.dkim19375.chatapp.common.util.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.OutputStream

import java.io.PrintStream
import java.util.stream.Stream

object SysStreamsLogger {
    private val sysOutLogger: Logger = LoggerFactory.getLogger("SYSOUT")
    private val sysErrLogger: Logger = LoggerFactory.getLogger("SYSERR")
    private val LINE_SEPARATOR = System.getProperty("line.separator")
    private const val DEFAULT_BUFFER_LENGTH = 2048

    fun bindSystemStreams() {
        System.setOut(PrintStream(LoggingOutputStream(sysOutLogger, System.out, false), true))
        System.setErr(PrintStream(LoggingOutputStream(sysErrLogger, System.err, true), true))
    }

    private class LoggingOutputStream(
        private val logger: Logger,
        private val original: PrintStream,
        private val isError: Boolean,
    ) : OutputStream() {

        private var hasBeenClosed = false
        private var buf: ByteArray = ByteArray(DEFAULT_BUFFER_LENGTH)
        private var count = 0
        private var bufLength = DEFAULT_BUFFER_LENGTH

        override fun close() {
            flush()
            hasBeenClosed = true
        }

        override fun write(b: Int) {
            if (isFromLogback(5)) {
                original.write(b)
                return
            }

            if (hasBeenClosed) {
                throw IOException("The stream has been closed.")
            }

            // don't log nulls
            if (b == 0) {
                return
            }

            // would this be writing past the buffer?
            if (count == bufLength) {
                // grow the buffer
                val newBufLength = bufLength + DEFAULT_BUFFER_LENGTH
                val newBuf = ByteArray(newBufLength)
                System.arraycopy(buf, 0, newBuf, 0, bufLength)
                buf = newBuf
                bufLength = newBufLength
            }
            buf[count] = b.toByte()
            count++
        }

        override fun flush() {
            if (isFromLogback(3)) {
                original.flush()
                return
            }

            if (count == 0) {
                return
            }

            // don't print out blank lines; flushing from PrintStream puts out
            // these
            if (count == LINE_SEPARATOR.length) {
                if (Char(buf[0].toUShort()) == LINE_SEPARATOR[0] && (count == 1 || count == 2 && Char(buf[1].toUShort()) == LINE_SEPARATOR[1])) {
                    reset()
                    return
                }
            }
            val theBytes = ByteArray(count)
            System.arraycopy(buf, 0, theBytes, 0, count)
            if (isError) {
                logger.error(String(theBytes))
            } else {
                logger.info(String(theBytes))
            }
            reset()
        }

        private fun reset() {
            // not resetting the buffer -- assuming that if it grew that it
            // will likely grow similarly again
            count = 0
        }

        private fun isFromLogback(skipAmount: Long): Boolean {
            val stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
            val frame = stackWalker.walk { stream1: Stream<StackWalker.StackFrame> ->
                stream1.skip(skipAmount).findFirst().orElse(null)
            }
            return frame.className.contains("logback", true)
        }
    }
}