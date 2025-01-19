package com.example.hearhere.service;

import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;
import org.springframework.stereotype.Service;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;

@Service
public class MusicDurationService {

    public String getWavDuration(String filePath) throws Exception {
        File file = new File(filePath);
        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);

        long frameLength = fileFormat.getFrameLength();
        float frameRate = fileFormat.getFormat().getFrameRate();
        return convertToMinutesAndSeconds((int) (frameLength / frameRate)); // 초 단위
    }

    public String getMp3Duration(String filePath) throws Exception {
        File file = new File(filePath);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
        MpegAudioFileReader reader = new MpegAudioFileReader();
        int frameLength = (int) audioInputStream.getFrameLength();
        float frameRate = audioInputStream.getFormat().getFrameRate();
        return convertToMinutesAndSeconds((int) (frameLength / frameRate));
    }

    public static String convertToMinutesAndSeconds(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }
}
