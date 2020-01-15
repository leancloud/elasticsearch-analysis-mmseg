package com.chenlb.mmseg4j.analysis;

import com.chenlb.mmseg4j.MMSeg;
import com.chenlb.mmseg4j.Seg;
import com.chenlb.mmseg4j.Word;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import java.io.IOException;
import java.io.Reader;

public class MMSegTokenizer extends Tokenizer {

	private ThreadLocal<MMSeg> mmSeg;

	private CharTermAttribute termAtt;
	private OffsetAttribute offsetAtt;
	private TypeAttribute typeAtt;

	/**
	 * The last offset
	 * 
	 * For multi-value field, e.g. `["图书管" "图书"]`, lucene expects the offset
	 * going incrementally, instead of restarting from zero. So we have to
	 * remember the last offset and add it when jumping to next value.
	 */
	private int lastOffset = 0;

	public MMSegTokenizer(Seg seg) {
		super();
		mmSeg =new ThreadLocal<>();
		mmSeg.set(new MMSeg(input, seg));

		termAtt = addAttribute(CharTermAttribute.class);
		offsetAtt = addAttribute(OffsetAttribute.class);
		typeAtt = addAttribute(TypeAttribute.class);
	}

	@Override
	public void reset() throws IOException {
        super.reset();
		//lucene 4.0
		//org.apache.lucene.analysis.Tokenizer.setReader(Reader)
		//setReader 自动被调用, input 自动被设置。
		mmSeg.get().reset(input);
	}

	/**
	 * End of the tokenize for a field
	 */
	@Override
	public void end() throws IOException {
		super.end();

		offsetAtt.setOffset(lastOffset, lastOffset);

		// all tokens from field are consumed, reset to zero
		lastOffset = 0;
	}

/*//lucene 2.9 以下
 	public Token next(Token reusableToken) throws IOException {
		Token token = null;
		Word word = mmSeg.next();
		if(word != null) {
			//lucene 2.3
			reusableToken.clear();
			reusableToken.setTermBuffer(word.getSen(), word.getWordOffset(), word.getLength());
			reusableToken.setStartOffset(word.getStartOffset());
			reusableToken.setEndOffset(word.getEndOffset());
			reusableToken.setType(word.getType());

			token = reusableToken;

			//lucene 2.4
			//token = reusableToken.reinit(word.getSen(), word.getWordOffset(), word.getLength(), word.getStartOffset(), word.getEndOffset(), word.getType());
		}

		return token;
	}*/

	//lucene 2.9/3.0
	@Override
	public final boolean incrementToken() throws IOException {
		clearAttributes();
		Word word = mmSeg.get().next();
		if(word != null) {
			//lucene 3.0
			//termAtt.setTermBuffer(word.getSen(), word.getWordOffset(), word.getLength());
			//lucene 3.1
			termAtt.copyBuffer(word.getSen(), word.getWordOffset(), word.getLength());
			offsetAtt.setOffset(lastOffset+word.getStartOffset(), lastOffset + word.getEndOffset());
			typeAtt.setType(word.getType());
			return true;
		} else {
			lastOffset += mmSeg.get().getReadIdx() + 1;
			return false;
		}
	}
}
