package vn.edu.hust.kinhlup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import vn.edu.hust.kinhlup.text.VietnameseNormalizer;

import static org.junit.jupiter.api.Assertions.*;

public class VietnameseNormalizerTest {
    private VietnameseNormalizer normalizer;

    @BeforeEach 
    public void setUp() throws Exception {
        normalizer = new VietnameseNormalizer();
    }

    @Test
    public void testNormalizeCase() throws Exception {

        String text = "ÔNG NGUYỄN KHẮC CHÚC LÀM VIỆC TẠI ĐẠI HỌC QUỐC GIA HÀ NỘI";


        String actual = normalizer.normalizeCase(text);

        String expected = "ông nguyễn khắc chúc làm việc tại đại học quốc gia hà nội";

        assertEquals(expected, actual);
    }

    @Test
    public void testNormalizeDiacritics() throws Exception {

        String input =
                "Tòan nhà, hoà bình. " +
                "(thuỷ sản); " +
                "uý quỳên! " +
                "\"tòan cảnh\"? " +
                "[chuỵên cũ]... " +
                "gìa, qụa: thủơ.";

        String actual = normalizer.normalizeDiacritics(input);

        String expected =
                "Toàn nhà, hòa bình. " +
                "(thủy sản); " +
                "úy quyền! " +
                "\"toàn cảnh\"? " +
                "[chuyện cũ]... " +
                "già, quạ: thuở.";

        assertEquals(expected, actual);
    }

    @Test
    public void testNormalizeSpelling() throws Exception {

        String input =
                "hoan hỷ " +
                "lý thuyết " +
                "huy hiệu " +
                "mỹ thuật " +
                "ý chí " +
                "sỹ số.";

        String actual = normalizer.normalizeSpelling(input);

        String expected =
                "hoan hỉ " +
                "lí thuyết " +
                "huy hiệu " +
                "mĩ thuật " +
                "ý chí " +
                "sĩ số.";

        assertEquals(expected, actual);
    }

    @Test
    public void testWordSegmentation() throws Exception {

        String text = 
                "Ông Nguyễn Khắc Chúc  đang làm việc tại Đại học Quốc gia Hà Nội. " +
                "Bà Lan, vợ ông Chúc, cũng làm việc tại đây.";


        String actual = normalizer.tokenize(text);

        String expected =
                "Ông Nguyễn_Khắc_Chúc đang làm_việc tại Đại_học Quốc_gia Hà Nội. " +
                "Bà Lan, vợ ông Chúc, cũng làm_việc tại đây.";

        assertEquals(expected, actual);
    }

    @Test
    public void testRemoveAccent() throws Exception {
        String text = 
                "Ông Nguyễn Khắc Chúc  đang làm việc tại Đại học Quốc gia Hà Nội. " +
                "Bà Lan, vợ ông Chúc, cũng làm việc tại đây.";


        String actual = normalizer.removeAccent(text);

        String expected =
                "Ong Nguyen Khac Chuc  dang lam viec tai Dai hoc Quoc gia Ha Noi. " +
                "Ba Lan, vo ong Chuc, cung lam viec tai day.";

        assertEquals(expected, actual);
    }

    @Test
    public void testNormalize() {
        String text =
                "ÔNG Tòan làm việc tại ĐẠI HỌC QUỐC GIA HÀ NỘI, "
                + "và là một KỸ SƯ giỏi.";

        String actual = normalizer.normalize(text);

        String expected =
                "ông toàn làm việc tại đại học quốc gia hà nội, "
                + "và là một kĩ sư giỏi.";

        assertEquals(expected, normalizer.normalize(actual));
    }
}