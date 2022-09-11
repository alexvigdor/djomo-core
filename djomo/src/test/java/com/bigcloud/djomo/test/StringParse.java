package com.bigcloud.djomo.test;

import java.io.IOException;

import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.io.Buffer;
import com.bigcloud.djomo.simple.StringModel;

public class StringParse {
	public static void main(String[] args) {
		try {
			StringModel model = (StringModel) new Models().stringModel;
			String input = "abcdefghijklmnopqrstuvwxyz\"";
			Buffer inputBuffer = new Buffer(input.toCharArray());
			inputBuffer.writePosition = input.length();
			Buffer overflow = new Buffer(new char[100]);
			String result = model.parse(inputBuffer, overflow);
			System.out.println(result);
			//warmup
			long time1 = System.currentTimeMillis();
			for(int i=0;i<10000000;i++) {
				inputBuffer.readPosition = 0;
				model.parse(inputBuffer, overflow);
			}
			long time2 = System.currentTimeMillis();
			System.out.println("Warmed up in "+(time2-time1));
			long time3 = System.currentTimeMillis();
			for(int i=0;i<100000000;i++) {
				inputBuffer.readPosition = 0;
				model.parse(inputBuffer, overflow);
			}
			long time4 = System.currentTimeMillis();
			System.out.println("Ran in "+(time4-time3));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
