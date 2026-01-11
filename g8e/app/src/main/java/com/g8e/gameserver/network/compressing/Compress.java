package com.g8e.gameserver.network.compressing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import com.g8e.util.Logger;

public class Compress {
    public static byte[] compress(String data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DeflaterOutputStream dos = new DeflaterOutputStream(baos, new Deflater(Deflater.BEST_COMPRESSION))) {
            dos.write(data.getBytes());
            dos.close();
            return baos.toByteArray();
        } catch (IOException e) {
            Logger.printError(e.getMessage());
            return null;
        }
    }
}
