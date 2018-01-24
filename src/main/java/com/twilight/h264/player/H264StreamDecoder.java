package com.twilight.h264.player;

import static com.twilight.h264.decoder.H264Context.NAL_AUD;
import static com.twilight.h264.decoder.H264Context.NAL_IDR_SLICE;
import static com.twilight.h264.decoder.H264Context.NAL_SLICE;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.function.Consumer;

import com.twilight.h264.decoder.AVFrame;
import com.twilight.h264.decoder.AVPacket;
import com.twilight.h264.decoder.H264Decoder;
import com.twilight.h264.decoder.MpegEncContext;
import com.twilight.h264.util.FrameUtils;

public final class H264StreamDecoder implements Runnable {
	public static final int INBUF_SIZE = 65536 * 16;
	private final InputStream stream;
	private final Consumer<BufferedImage> frameListener;
	private final boolean closeAfterDecode;
	
	
	public H264StreamDecoder(InputStream stream,
			Consumer<BufferedImage> frameListener,
			boolean closeAfterDecode) {
		this.stream = stream;
		this.frameListener = frameListener;
		this.closeAfterDecode = closeAfterDecode;
	}

	@Override
	public void run() {
		try {
			decodeStream(stream);
		} finally {
			if(closeAfterDecode) {
				try { stream.close(); } catch (IOException e) { e.printStackTrace(); }
			}
		}
	}
	
	private boolean isEndOfFrame(int code, BooleanHolder foundFrameStart) {
		int nal = code & 0x1F;

		if (nal == NAL_AUD) {
			foundFrameStart.set(false);
			return true;
		}

		boolean foundFrame = foundFrameStart.get();
		if (nal == NAL_SLICE || nal == NAL_IDR_SLICE) {
			if (foundFrameStart.get()) {
				return true;
			}
			foundFrameStart.set(true);
		} else {
			foundFrameStart.set(false);
		}
		return foundFrame;
	}

	public void decodeStream(InputStream stream) {
	    int[] buffer = null;
	    int frame, len;
	    int[] got_picture = new int[1];
	    AVFrame picture;

	    byte[] inbuf = new byte[INBUF_SIZE + MpegEncContext.FF_INPUT_BUFFER_PADDING_SIZE];
	    int[] inbuf_int = new int[INBUF_SIZE + MpegEncContext.FF_INPUT_BUFFER_PADDING_SIZE];
	    AVPacket avpkt = new AVPacket();

	    avpkt.av_init_packet();

	    /* set end of buffer to 0 (this ensures that no overreading happens for damaged mpeg streams) */
	    Arrays.fill(inbuf, INBUF_SIZE, MpegEncContext.FF_INPUT_BUFFER_PADDING_SIZE + INBUF_SIZE, (byte)0);


	    /* find the mpeg1 video decoder */
	    final H264Decoder codec  = new H264Decoder();

	    final MpegEncContext context = MpegEncContext.avcodec_alloc_context();
	    picture = AVFrame.avcodec_alloc_frame();

	    if((codec.capabilities & H264Decoder.CODEC_CAP_TRUNCATED) != 0)
	        context.flags |= MpegEncContext.CODEC_FLAG_TRUNCATED; /* we do not send complete frames */

	    /* For some codecs, such as msmpeg4 and mpeg4, width and height
	       MUST be initialized there because this information is not
	       available in the bitstream. */

	    /* open it */
	    if (context.avcodec_open(codec) < 0) {
	    	System.out.println("could not open codec\n");
	        System.exit(1);
	    }

	    try {
		    /* the codec gives us the frame size, in samples */

		    frame = 0;
		    int dataPointer;
		    BooleanHolder foundFrameStart = new BooleanHolder(false);

		    // avpkt must contain exactly 1 NAL Unit in order for decoder to decode correctly.
	    	// thus we must read until we get next NAL header before sending it to decoder.
			// Find 1st NAL
			int[] cacheRead = new int[5];
			cacheRead[0] = stream.read();
			cacheRead[1] = stream.read();
			cacheRead[2] = stream.read();
			cacheRead[3] = stream.read();

			while(!(
					cacheRead[0] == 0x00 &&
					cacheRead[1] == 0x00 &&
					cacheRead[2] == 0x00 &&
					cacheRead[3] == 0x01
					)) {
				 cacheRead[0] = cacheRead[1];
				 cacheRead[1] = cacheRead[2];
				 cacheRead[2] = cacheRead[3];
				 cacheRead[3] = stream.read();
			} 

			boolean hasMoreNAL = true;
			cacheRead[4] = stream.read();

			// 4 first bytes always indicate NAL header
			while (hasMoreNAL) {
				inbuf_int[0] = cacheRead[0];
				inbuf_int[1] = cacheRead[1];
				inbuf_int[2] = cacheRead[2];
				inbuf_int[3] = cacheRead[3];
				inbuf_int[4] = cacheRead[4];

				dataPointer = 5;
				// Find next NAL
				cacheRead[0] = stream.read();
				if (cacheRead[0]==-1) hasMoreNAL = false;
				cacheRead[1] = stream.read();
				if (cacheRead[1]==-1) hasMoreNAL = false;
				cacheRead[2] = stream.read();
				if (cacheRead[2]==-1) hasMoreNAL = false;
				cacheRead[3] = stream.read();
				if (cacheRead[3]==-1) hasMoreNAL = false;
				cacheRead[4] = stream.read();
				if (cacheRead[4]==-1) hasMoreNAL = false;
				while(!(
						cacheRead[0] == 0x00 &&
						cacheRead[1] == 0x00 &&
						cacheRead[2] == 0x00 &&
						cacheRead[3] == 0x01 &&
						isEndOfFrame(cacheRead[4], foundFrameStart) 
						) && hasMoreNAL) {
					 inbuf_int[dataPointer++] = cacheRead[0];
					 cacheRead[0] = cacheRead[1];
					 cacheRead[1] = cacheRead[2];
					 cacheRead[2] = cacheRead[3];
					 cacheRead[3] = cacheRead[4];
					 cacheRead[4] = stream.read();
					if (cacheRead[4]==-1) hasMoreNAL = false;
				}

				avpkt.size = dataPointer;
				
		        avpkt.data_base = inbuf_int;
		        avpkt.data_offset = 0;

		        try {
			        while (avpkt.size > 0) {
			            len = context.avcodec_decode_video2(picture, got_picture, avpkt);
			            if (len < 0) {
			                System.out.println("Error while decoding frame "+ frame);
			                // Discard current packet and proceed to next packet
			                break;
			            }
			            if (got_picture[0] != 0) {
			            	picture = context.priv_data.displayPicture;

			            	int imageWidth = picture.imageWidthWOEdge;
			            	int imageHeight = picture.imageHeightWOEdge;
			            	
			            	
							int bufferSize = imageWidth * imageHeight;
							if (buffer == null || bufferSize != buffer.length) {
								buffer = new int[bufferSize];
							}
							FrameUtils.YUV2RGB_WOEdge(picture, buffer);
							BufferedImage im = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_3BYTE_BGR);
							im.setRGB(0, 0, imageWidth, imageHeight, buffer, 0, imageWidth);
							
							if(frameListener != null) {
								frameListener.accept(im);
							}
			            }
			            avpkt.size -= len;
			            avpkt.data_offset += len;
			        }
		        } catch(Exception ie) {
		        	ie.printStackTrace();
		        }

			}
	    } catch(Exception e) {
	    	e.printStackTrace();
	    } finally {
	    	context.avcodec_close();
	    }
	}
}

class BooleanHolder {
	private boolean value;
	public BooleanHolder() { }
	public BooleanHolder(boolean value) { this.value = value; }
	public boolean get() { return value; }
	public void set(boolean v) { this.value = v; }
}
