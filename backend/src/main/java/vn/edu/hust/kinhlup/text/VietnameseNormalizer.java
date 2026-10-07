package vn.edu.hust.kinhlup.text;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vn.corenlp.wordsegmenter.WordSegmenter;

public class VietnameseNormalizer {

    private final WordSegmenter wordSegmenter;

    public VietnameseNormalizer() throws IOException {
        this.wordSegmenter = new WordSegmenter();
    }

    // xóa dấu tiếng Việt
    public String removeAccent(String text) {
        if (text == null) {
            return null;
        }

        String normalized = Normalizer.normalize(
                text,
                Normalizer.Form.NFD
        );

        normalized = normalized
                .replace('đ', 'd')
                .replace('Đ', 'D');

        return normalized.replaceAll("\\p{M}", "");
    }

    // dùng vncorenlp để tách từ
    public String tokenize(String text) throws IOException {
        return wordSegmenter.segmentTokenizedString(text);
    }

    // chuẩn hóa tiếng việt
    public String normalize(String text) {
        if (text == null) {
            return null;
        }

        text = normalizeUnicode(text);
        text = normalizeCase(text);
        text = normalizeDiacritics(text);
        text = normalizeSpelling(text);

        return text;
    }

    // chuyển tất cả thành lowercase
    public String normalizeCase(String text) {
        return text.toLowerCase(Locale.ROOT);
    }

    // chuẩn hóa unicode
    private String normalizeUnicode(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFC);
    }

    //==================================================
    // Chuẩn hóa tiếng Việt về cách đặt dấu cũ:
    // https://vi.wikipedia.org/wiki/Quy_t%E1%BA%AFc_%C4%91%E1%BA%B7t_d%E1%BA%A5u_thanh_c%E1%BB%A7a_ch%E1%BB%AF_Qu%E1%BB%91c_ng%E1%BB%AF
    // Code tham khảo từ: https://gist.github.com/enamoria/e11edd8ec32863e2d83652f120c450c6
    //==================================================
    public String normalizeDiacritics(String text) {
                // dùng regex tìm
        Matcher matcher = WORD_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
    
        while (matcher.find()) {
            String word = matcher.group();
        
            // Xử lý riêng từng từ
            String correctedWord = correctVnAccentWord(word);
        
            matcher.appendReplacement(
                result,
                Matcher.quoteReplacement(correctedWord)
            );
        }
    
        matcher.appendTail(result);
    
        return result.toString();
    }

    private static final Pattern WORD_PATTERN = Pattern.compile("\\p{L}+");

    static Character[][] vowelTable = {
            {'a', 'à', 'á', 'ả', 'ã', 'ạ'},
            {'ă', 'ằ', 'ắ', 'ẳ', 'ẵ', 'ặ'},
            {'â', 'ầ', 'ấ', 'ẩ', 'ẫ', 'ậ'},
            {'e', 'è', 'é', 'ẻ', 'ẽ', 'ẹ'},
            {'ê', 'ề', 'ế', 'ể', 'ễ', 'ệ'},
            {'i', 'ì', 'í', 'ỉ', 'ĩ', 'ị'},
            {'o', 'ò', 'ó', 'ỏ', 'õ', 'ọ'},
            {'ô', 'ồ', 'ố', 'ổ', 'ỗ', 'ộ'},
            {'ơ', 'ờ', 'ớ', 'ở', 'ỡ', 'ợ'},
            {'u', 'ù', 'ú', 'ủ', 'ũ', 'ụ'},
            {'ư', 'ừ', 'ứ', 'ử', 'ữ', 'ự'},
            {'y', 'ỳ', 'ý', 'ỷ', 'ỹ', 'ỵ'}
    };
    static Set<Character> vietnamChars;
    static Map<Character, Integer> vowelLookupRow = new HashMap<>();
    static Map<Character, Integer> vowelLookupColumn = new HashMap<>();

    static {
        for (int i = 0; i < vowelTable.length; i++) {
            for (int j = 0; j < vowelTable[i].length; j++) {
                vowelLookupRow.put(vowelTable[i][j], i);
                vowelLookupColumn.put(vowelTable[i][j], j);
            }
        }

        vietnamChars = new HashSet<>(Arrays.asList('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
                'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
                'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'à', 'á', 'ả', 'ã',
                'ạ', 'ầ', 'ấ', 'ẩ', 'ẫ', 'ậ', 'ằ', 'ắ', 'ẳ', 'ẵ', 'ặ', 'è', 'é', 'ẻ', 'ẽ', 'ẹ', 'ề', 'ế', 'ể', 'ễ', 'ệ',
                'ì', 'í', 'ỉ', 'ĩ', 'ị', 'ò', 'ó', 'ỏ', 'õ', 'ọ', 'ô', 'ồ', 'ố', 'ổ', 'ỗ', 'ộ', 'ờ', 'ớ', 'ở', 'ỡ', 'ợ', 'ù',
                'ú', 'ủ', 'ũ', 'ụ', 'ừ', 'ứ', 'ử', 'ữ', 'ự', 'ỳ', 'ý', 'ỷ', 'ỹ', 'ỵ', 'À', 'Á', 'Ả', 'Ã', 'Ạ', 'Ầ', 'Ấ',
                'Ẩ', 'Ẫ', 'Ậ', 'Ằ', 'Ắ', 'Ẳ', 'Ẵ', 'Ặ', 'È', 'É', 'Ẻ', 'Ẽ', 'Ẹ', 'Ề', 'Ế', 'Ể', 'Ễ', 'Ệ', 'Ì', 'Í', 'Ỉ',
                'Ĩ', 'Ị', 'Ò', 'Ó', 'Ỏ', 'Õ', 'Ọ', 'Ô', 'Ồ', 'Ố', 'Ổ', 'Ỗ', 'Ộ', 'Ờ', 'Ớ', 'Ở', 'Ỡ', 'Ợ', 'Ù', 'Ú', 'Ủ', 'Ũ',
                'Ụ', 'Ừ', 'Ứ', 'Ử', 'Ữ', 'Ự', 'Ỳ', 'Ý', 'Ỷ', 'Ỹ', 'Ỵ', 'đ', 'Đ', 'ă', 'Ă', 'â', 'Â', 'ê', 'Ê', 'ô', 'Ô', 'ơ', 'Ơ', 'ư', 'Ư'));
    }

    private static String correctVnAccentWord(String word) {

        char[] chars = word.toCharArray();
        int accentPosition = 0, x, y;
        boolean isQuOrGi = false;

        List<Integer> vowelsIndex = new ArrayList<>();
        for (int i = 0; i < chars.length; i++) {
            x = vowelLookupRow.getOrDefault(chars[i], -1);
            y = vowelLookupColumn.getOrDefault(chars[i], -1);

            if (x == -1) continue;
            else if (x == 9) { // qu
                if (i != 0 && chars[i - 1] == 'q') {
                    chars[i] = 'u';
                    isQuOrGi = true;
                }
            } else if (x == 5) { // gi
                if (i != 0 && chars[i - 1] == 'g') {
                    chars[i] = 'i';
                    isQuOrGi = true;
                }
            }
            if (y != 0) {
                accentPosition = y;
                chars[i] = vowelTable[x][0];
            }
            // qu và gi không tính là nguyên âm
            if (!isQuOrGi || i != 1) {
                vowelsIndex.add(i);
            }
        }
        if (vowelsIndex.size() < 2) {
            if (isQuOrGi) {
                // gì
                if (chars.length == 2) {
                    x = vowelLookupRow.get(chars[1]);
                    chars[1] = vowelTable[x][accentPosition];
                } else {
                    x = vowelLookupRow.getOrDefault(chars[2], -1);
                    // giá
                    if (x != -1) {
                        chars[2] = vowelTable[x][accentPosition];
                    }
                    // gìn
                    else {
                        chars[1] = (chars[1] == 'i' ? vowelTable[5][accentPosition] : vowelTable[9][accentPosition]);
                    }
                }
                return String.copyValueOf(chars);
            }
            return word;
        }
        for (int index : vowelsIndex) {
            x = vowelLookupRow.get(chars[index]);
            // nếu có ê, ơ thì đặt dấu vào ê, ơ
            if (x == 4 || x == 8) { // ê, ơ
                chars[index] = vowelTable[x][accentPosition];
                return String.copyValueOf(chars);
            }
        }
        if (vowelsIndex.size() == 2) {
            // sau 2 nguyên âm không còn phụ âm đặt dấu vào nguyên âm đầu tiên
            if (vowelsIndex.get(vowelsIndex.size() - 1) == chars.length - 1) {
                x = vowelLookupRow.get(chars[vowelsIndex.get(0)]);
                chars[vowelsIndex.get(0)] = vowelTable[x][accentPosition];
            } 
            // sau 2 nguyên âm còn phụ âm đặt dấu vào nguyên âm thứ hai
            else {
                x = vowelLookupRow.get(chars[vowelsIndex.get(1)]);
                chars[vowelsIndex.get(1)] = vowelTable[x][accentPosition];
            }
        } 
        // nhiều hơn 2 nguyên âm đặt dấu vào nguyên âm thứ hai
        else {
            x = vowelLookupRow.get(chars[vowelsIndex.get(1)]);
            chars[vowelsIndex.get(1)] = vowelTable[x][accentPosition];
        }
        return String.copyValueOf(chars);
    }

    //==================================================
    // Chuẩn hóa chính tả:
    // Chuyển y dài thành i ngắn nếu đứng sau phụ âm h, k, l, m, s, t
    // Ở đây chỉ chuẩn hóa những từ gồm 1 phụ âm và 1 nguyên âm i
    // Liệu có từ nào nhiều hơn 2 kí tự cần chuẩn hóa không?
    //==================================================
    public String normalizeSpelling(String text) {
        // dùng regex tìm từ
        Matcher matcher = WORD_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
    
        while (matcher.find()) {
            String word = matcher.group();
        
            // Xử lý riêng từng từ
            String normalizedWord = normalizeIY(word);
        
            matcher.appendReplacement(
                result,
                Matcher.quoteReplacement(normalizedWord)
            );
        }
    
        matcher.appendTail(result);
    
        return result.toString();
    }

    private static String normalizeIY(String word) {
        if (word == null || word.length() != 2) {
            return word;
        }

        char first = word.charAt(0);
        char second = word.charAt(1);

        if ("hklmst".indexOf(first) == -1) {
            return word;
        }

        switch (second) {
            case 'y':
                return word.substring(0, 1) + 'i';
            case 'ý':
                return word.substring(0, 1) + 'í';
            case 'ỳ':
                return word.substring(0, 1) + 'ì';
            case 'ỷ':
                return word.substring(0, 1) + 'ỉ';
            case 'ỹ':
                return word.substring(0, 1) + 'ĩ';
            case 'ỵ':
                return word.substring(0, 1) + 'ị';
            default:
                return word;
        }
    }

}
