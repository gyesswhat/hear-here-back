package com.example.hearhere.common.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArrayParser {

    // String을 ArrayList<String>으로 변환하는 함수
    public static ArrayList<String> parseStringToArrayList(String arrayString) {
        // 입력된 문자열에서 대괄호 제거
        String cleanedString = arrayString.replaceAll("\\[|\\]", "");

        // 쉼표를 기준으로 분리하고 각 요소에 대해 공백을 제거하여 리스트로 변환
        List<String> items = Arrays.asList(cleanedString.split("\\s*,\\s*"));

        // ArrayList로 변환
        return new ArrayList<>(items);
    }

    // String을 ArrayList<Integer>로 변환하는 함수
    public static ArrayList<Integer> parseStringToIntegerArrayList(String arrayString) {
        // 입력된 문자열에서 대괄호 제거
        String cleanedString = arrayString.replaceAll("\\[|\\]", "");

        // 쉼표를 기준으로 분리하고 각 요소에 대해 공백을 제거하여 리스트로 변환
        List<String> items = Arrays.asList(cleanedString.split("\\s*,\\s*"));

        // String 리스트를 Integer 리스트로 변환
        ArrayList<Integer> intList = new ArrayList<>();
        for (String item : items) {
            try {
                intList.add(Integer.parseInt(item)); // 각 요소를 Integer로 변환하여 추가
            } catch (NumberFormatException e) {
                System.err.println("Error parsing integer: " + item);
            }
        }
        return intList;
    }}
