/**
 * 
 */
package com.yacme.ext.oxsit.cust_it.comp.security.xades;

import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.StringTokenizer;
//import org.apache.log4j.Logger;

/**
 * Base64 utility methods. 
 * Based on an implementation by Apache,
 * but changed to suit the needs of this
 * project.
 */
public class Base64Util  {
	/** log4j logger */
	//FIXME: set the oxsit private logger
//	private static Logger m_logger = Logger.getLogger(Base64Util.class);;
    public static final int BASE64DEFAULTLENGTH = 64;
    public static final String LINE_SEPARATOR = "\n";
    static String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
	
    /**
     * Encode a byte array and fold lines at the standard 76th character.
     *
     * @param raw <code>byte[]<code> to be base64 encoded
     * @return the <code>String<code> with encoded data
     */
    public static String encode(byte[] raw) {
        return encode(raw, Base64Util.BASE64DEFAULTLENGTH);
    }
    
    /**
     * <p>Encode a byte array in Base64 format and return an optionally
     * wrapped line</p>
     *
     * @param raw <code>byte[]</code> data to be encoded
     * @param wrap <code>int<code> length of wrapped lines; No wrapping if less than 4.
     * @return a <code>String</code> with encoded data
     */
    public static String encode(byte[] raw, int wrap) {
        //calculate length of encoded string
        int encLen = ((raw.length + 2) / 3) * 4;
        //adjust for newlines
        if (wrap > 3) {
            wrap -= wrap % 4;
            encLen += 2 * (encLen / wrap);
        } else {    //disable wrapping
            wrap = Integer.MAX_VALUE;
        }
        StringBuffer encoded = new StringBuffer(encLen);
        int len3 = (raw.length / 3) * 3;
        int outLen = 0;    //length of output line
        
        for (int i = 0; i < len3; i += 3, outLen += 4) {
            if (outLen + 4 > wrap) {
                encoded.append(LINE_SEPARATOR);
                
                outLen = 0;
            }
            //System.out.println("Encode offset: " + i);
            encoded.append(encodeFullBlock(raw, i));
        }
        if (outLen >= wrap) {    //this will produce an extra newline if needed !? Sun had it this way...
            encoded.append(LINE_SEPARATOR);
        }
        if (len3 < raw.length) {
            //System.out.println("Encode offset: " + len3);
            encoded.append(encodeBlock(raw, raw.length, len3));
        }
        return encoded.toString();
    }
    
    /**
     * <p>Encodes a byte array in Base64 format and writes to
     * StringBuffer object. For regular blocks writes only full
     * lines and leaves the rest unused. Caller should check
     * how many bytes were actually enocded and return the rest
     * at the beginning of the next block</p>
     *
     * @param raw <code>byte[]</code> data to be encoded
     * @param rawLen number of bytes used in raw
     * @param sb output buffer
     * @param bLastBlock true if this is the last block to write
     * @return number of raw bytes encoded and written to output stream. Thus
     * 
     */
    public static int encodeToBlock(byte[] raw, int rawLen, StringBuffer sb, boolean bLastBlock) 
    	throws IOException
    {
    	int wrap = Base64Util.BASE64DEFAULTLENGTH, outLen = 0;
        int len3 = (rawLen / 3) * 3;
        int nBytesPerLine = (wrap / 4) * 3;
        int nUsedBytes = 0, nTotal = 0;
        
        //if(m_logger.isDebugEnabled())
        //	m_logger.debug("Encoding: " + raw.length + " bytes, last-block: " + bLastBlock);
        if(!bLastBlock) {
        	nUsedBytes = (rawLen / nBytesPerLine) * nBytesPerLine;
        	for (int i = 0; i < nUsedBytes; i += 3, outLen += 4) {
                if (outLen + 4 > wrap) {
                	sb.append(LINE_SEPARATOR);
                	nTotal++;
                    outLen = 0;
                }
                //System.out.println("Encode offset: " + i);
                char[] encdata = encodeFullBlock(raw, i);
                nTotal += encdata.length;
                sb.append(encdata);
            } 
        	if (outLen >= wrap) {    //this will produce an extra newline if needed !? Sun had it this way...
        		sb.append(LINE_SEPARATOR);
        		nTotal++;
        	}
        }
        else {
        	nUsedBytes = rawLen;
        	for (int i = 0; i < len3; i += 3, outLen += 4) {
        		if (outLen + 4 > wrap) {
        			sb.append(LINE_SEPARATOR);
        			outLen = 0;
        			nTotal++;
        		}
        		char[] encdata = encodeFullBlock(raw, i); 
        		nTotal += encdata.length;
        		sb.append(encdata);
        	}
        	if (outLen >= wrap) {    //this will produce an extra newline if needed !? Sun had it this way...
        		sb.append(LINE_SEPARATOR);
        		nTotal++;
        	}
        	if (len3 < rawLen) {
        		char[] encdata = encodeBlock(raw, rawLen, len3);
        		nTotal += encdata.length;
        		sb.append(encdata);
        	}
        }
//        if(m_logger.isDebugEnabled())
//        	m_logger.debug("Input: " + rawLen + " used: " + nUsedBytes + " last: " + bLastBlock + " wrote: " + nTotal);
        return nUsedBytes;
    }
    
    /**
     * <p>Encodes a byte array in Base64 format and writes to
     * output stream. For regular blocks writes only full
     * lines and leaves the rest unused. Caller should check
     * how many bytes wre actually enocded and return the rest
     * at the beginning of the next block</p>
     *
     * @param raw <code>byte[]</code> data to be encoded
     * @param outs output stream
     * @param bLastBlock true if this is the last block to write
     * @return number of raw bytes encoded and written to output stream. Thus
     * 
     */
    public static int encodeToStream(byte[] raw, OutputStream outs, boolean bLastBlock) 
    	throws IOException
    {
    	int wrap = Base64Util.BASE64DEFAULTLENGTH, outLen = 0;
        int len3 = (raw.length / 3) * 3;
        int nBytesPerLine = (wrap / 4) * 3;
        int nUsedBytes = 0, nTotal = 0;
        
        //if(m_logger.isDebugEnabled())
        //	m_logger.debug("Encoding: " + raw.length + " bytes, last-block: " + bLastBlock);
        if(!bLastBlock) {
        	nUsedBytes = (raw.length / nBytesPerLine) * nBytesPerLine;
        	for (int i = 0; i < nUsedBytes; i += 3, outLen += 4) {
                if (outLen + 4 > wrap) {
                	outs.write(LINE_SEPARATOR.getBytes());
                	nTotal++;
                    outLen = 0;
                }
                //System.out.println("Encode offset: " + i);
                char[] encdata = encodeFullBlock(raw, i);
                nTotal += encdata.length;
                outs.write(new String(encdata).getBytes());
            } 
        	if (outLen >= wrap) {    //this will produce an extra newline if needed !? Sun had it this way...
        		outs.write(LINE_SEPARATOR.getBytes());
        		nTotal++;
        	}
        }
        else {
        	nUsedBytes = raw.length;
        	for (int i = 0; i < len3; i += 3, outLen += 4) {
        		if (outLen + 4 > wrap) {
        			outs.write(LINE_SEPARATOR.getBytes());
        			outLen = 0;
        			nTotal++;
        		}
        		char[] encdata = encodeFullBlock(raw, i); 
        		nTotal += encdata.length;
        		outs.write(new String(encdata).getBytes());
        	}
        	if (outLen >= wrap) {    //this will produce an extra newline if needed !? Sun had it this way...
        		outs.write(LINE_SEPARATOR.getBytes());
        		nTotal++;
        	}
        	if (len3 < raw.length) {
        		char[] encdata = encodeBlock(raw, raw.length, len3);
        		nTotal += encdata.length;
        		outs.write(new String(encdata).getBytes());
        	}
        }
//        if(m_logger.isDebugEnabled())
//        	m_logger.debug("Encoded: " + raw.length + " last: " + bLastBlock + " wrote: " + nTotal);
        return nUsedBytes;
    }
    
    /**
     * Method encodeBlock
     *
     * @param raw
     * @param offset
     * @return
     */
    protected static char[] encodeBlock(byte[] raw, int rawLen, int offset) {
        int block = 0;
        int slack = rawLen - offset - 1;
//        if(m_logger.isDebugEnabled())
//        	m_logger.debug("raw: " + rawLen + " offset " + offset + " slack: " + slack);
        int end = (slack >= 2) ? 2 : slack;
        for (int i = 0; i < 3; i++) {
            byte b = (offset + i < raw.length) ? raw[offset + i] : 0;
            int neuter = (b < 0) ? b + 256 : b;
            block <<= 8;
            block += neuter;
        }
        char[] base64 = new char[4];
        for (int i = 3; i >= 0; i--) {
            int sixBit = block & 0x3f;
            base64[i] = getChar(sixBit);
            block >>= 6;
        }
        if (slack < 1)
            base64[2] = '=';
        if (slack < 2)
            base64[3] = '=';
        return base64;
    }
    
    /**
     * Method encodeFullBlock
     *
     * @param raw
     * @param offset
     * @return
     */
    protected static char[] encodeFullBlock(byte[] raw, int offset) {
        int block = 0;
        for (int i = 0; i < 3; i++) {
            block <<= 8;
            block += (0xff & raw[offset + i]);
        }
        block = ((raw[offset] & 0xff) << 16) + ((raw[offset + 1] & 0xff) << 8)
        + (raw[offset + 2] & 0xff);
        char[] base64 = new char[4];
        for (int i = 3; i >= 0; i--) {
            int sixBit = block & 0x3f;
            base64[i] = getChar(sixBit);
            block >>= 6;
        }
        return base64;
    }
    
    /**
     * Method getChar
     *
     * @param sixBit
     * @return
     */
    protected static char getChar(int sixBit) {
        if ((sixBit >= 0) && (sixBit < 26))
            return (char) ('A' + sixBit);
        if ((sixBit >= 26) && (sixBit < 52))
            return (char) ('a' + (sixBit - 26));
        if ((sixBit >= 52) && (sixBit < 62))
            return (char) ('0' + (sixBit - 52));
        if (sixBit == 62)
            return '+';
        if (sixBit == 63)
            return '/';
        return '?';
    }
    
    /**
     * Decodes base64 block and ignores all whitespace.
     * @param base64 base64 input data
     * @return decoded data
     */
    public static byte[] decode(byte[] base64) {
    	// try to allocate mem in one step to avoid fragmentation
    	ByteArrayOutputStream bos = new ByteArrayOutputStream(base64.length / 4 * 3);
    	char[] four = new char[4];
		int i = 0, j, aux;
		do {
			j = 0;
			while(j < 4 && i < base64.length) {
				char c = (char)base64[i];
				// ignore whitespace and padding
				if(c != ' ' && c != '\t' && c != '\n' && c != '=' && c != '\r') {
					four[j] = c;
					j++;
				}
				i++;
			}
			if(j > 0) {
				aux = 0;
				for(int k = 0; k < j; k++)
					aux = aux | (chars.indexOf(four[k]) << (6 * (3 - k)));
				for(int m = 0; m < j-1; m++) {
					byte b = (byte)((aux >>> (8 * (2 - m))) & 0xFF);
					bos.write(b);
				}
			}
		} while(i < base64.length);
		return bos.toByteArray();
    	/* old version
        try {
            return decode(new String(base64, "UTF-8"));
        } catch (java.io.UnsupportedEncodingException ex) {
            // should never be reached because Encoding is valid and fixed
            return null;
        }*/
    }
    
    /**
     * <p>Decode a Base64-encoded string to a byte array
     * and writes decoded data to output stream. Returns
     * the number of bytes from input data used. Caller must
     * pass in the unused bytes on the next call.
     * </p>
     * @param base64 <code>String</code> encoded string. Whitespace will
     * be ignored.
     * @param out output stream to write decoded data
     * @param bLastBlock true if this is the last block of input data
     * @return number of handled bytes from input data
     */
    public static int decodeBlock(String base64, OutputStream out, boolean bLastBlock) 
    {
    	int nUsed = 0, nPos = 0, nDec = 0;
    	StringBuffer sbBlock = null;
    	do {
    		// collect the next 4 characters, skip whitespace
    		sbBlock = new StringBuffer();
    		while(nPos < base64.length() && sbBlock.length() < 4) {
    			char ch = base64.charAt(nPos);
    			if(ch != ' ' && ch != '\n' && ch != '\t' && ch != '\r')
    				sbBlock.append(ch);
    			nPos++;
    		}
    		// if last block then pad
    		while(bLastBlock && sbBlock.length() < 4)
    			sbBlock.append('=');
    		// decode if possible
    		if(sbBlock.length() == 4) {
    			//byte[] decdata = decodeWithoutWhitespace(sbBlock.toString());
    			int block = (getValue(sbBlock.charAt(0)) << 18)
	            	+ (getValue(sbBlock.charAt(1)) << 12)
					+ (getValue(sbBlock.charAt(2)) << 6)
					+ (getValue(sbBlock.charAt(3)));
    			byte[] decdata = new byte[3];
	            for (int j = 2; j >= 0; j--) {
	            	decdata[j] = (byte) (block & 0xff);
	                block >>= 8;
	            }
    			nDec += decdata.length;
    			try {
    				out.write(decdata);
    			} catch(IOException ex) {
    				
    			}
    			nUsed = nPos;
    		}
    		
    	} while(nPos < base64.length());
//    	if(m_logger.isDebugEnabled())
//    		m_logger.debug("Decoding: " + base64.length() + " last: " + bLastBlock + " used: " + nUsed + " decoded: " + nDec);
        return nUsed;
    }
    
    
    /**
     * <p>Decode a Base64-encoded string to a byte array</p>
     *
     * @param base64 <code>String</code> encoded string (single line only !!)
     * @return Decoded data in a byte array
     */
    public static byte[] decode(String base64) {
        if (base64.length() < 30) {
            //cat.debug("I was asked to decode \"" + base64 + "\"");
        } else {
            //cat.debug("I was asked to decode \"" + base64.substring(0, 20) + "...\"");
        }
        //strip whitespace from anywhere in the string.  Not the most memory
        //efficient solution but elegant anyway :-)      
        StringTokenizer tok = new StringTokenizer(base64, " \n\r\t", false);
        StringBuffer buf = new StringBuffer(base64.length());
        while (tok.hasMoreElements()) {
            buf.append(tok.nextToken());
        }
        base64 = buf.toString();
       
        int pad = 0;
        for (int i = base64.length() - 1; (i > 0) && (base64.charAt(i) == '='); i--) {
            pad++;
        }
        int length = base64.length() / 4 * 3 - pad;
        byte[] raw = new byte[length];
        for (int i = 0, rawIndex = 0; i < base64.length(); i += 4, rawIndex += 3) {
            int block = (getValue(base64.charAt(i)) << 18)
            + (getValue(base64.charAt(i + 1)) << 12)
            + (getValue(base64.charAt(i + 2)) << 6)
            + (getValue(base64.charAt(i + 3)));
            for (int j = 2; j >= 0; j--) {
                if (rawIndex + j < raw.length) {
                    raw[rawIndex + j] = (byte) (block & 0xff);
                }
                block >>= 8;
            }
        }
        return raw;
    }

    /**
     * <p>Decode a Base64-encoded string to a byte array</p>
     * This works only if you have stripped whitespace yourself
     * @param base64 <code>String</code> encoded string (single line only !!)
     * @return Decoded data in a byte array
     */
    public static byte[] decodeWithoutWhitespace(String base64) {
        int pad = 0;
        for (int i = base64.length() - 1; (i > 0) && (base64.charAt(i) == '='); i--) {
            pad++;
        }
        int length = base64.length() / 4 * 3 - pad;
        byte[] raw = new byte[length];
        for (int i = 0, rawIndex = 0; i < base64.length(); i += 4, rawIndex += 3) {
            int block = (getValue(base64.charAt(i)) << 18)
            + (getValue(base64.charAt(i + 1)) << 12)
            + (getValue(base64.charAt(i + 2)) << 6)
            + (getValue(base64.charAt(i + 3)));
            for (int j = 2; j >= 0; j--) {
                if (rawIndex + j < raw.length) {
                    raw[rawIndex + j] = (byte) (block & 0xff);
                }
                block >>= 8;
            }
        }
        return raw;
    }
    
    /**
     * Method getValue
     *
     * @param c
     * @return
     */
    protected static int getValue(char c) {
        if ((c >= 'A') && (c <= 'Z'))
            return c - 'A';
        if ((c >= 'a') && (c <= 'z'))
            return c - 'a' + 26;
        if ((c >= '0') && (c <= '9'))
            return c - '0' + 52;
        if (c == '+')
            return 62;
        if (c == '/')
            return 63;
        if (c == '=')
            return 0;
        return -1;
    }
    
}
