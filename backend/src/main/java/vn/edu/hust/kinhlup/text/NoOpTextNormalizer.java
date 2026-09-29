package vn.edu.hust.kinhlup.text;

import org.springframework.stereotype.Component;

@Component
public class NoOpTextNormalizer implements TextNormalizer {

    @Override
    public String normalize(String text) {
        return text;
    }
}
