#!/usr/bin/env jq
# Useful jq functions for writing tests with expectations for JSON values
# Note that EXPECT_*/3 functions are designed to be used as third expectations argument to TEST/3

def TEST(description; valuesToTest; expectations):
    try (
        { what: description
        , valueUnderTest:
            ( if type == "object" and (keys | length) == 2 and has("what") and has("valueUnderTest")
            then .valueUnderTest | valuesToTest  # gracefully handle nested TEST
            else valuesToTest
            end)
        } | expectations
    ) catch error("Unexpected \(description): \(.)");

def EXPECT_EQ(expectationDescription; actualValue; expectedValue):
    if .valueUnderTest | [actualValue == expectedValue] | all then "PASS: \(expectationDescription)"
    else error("
FAIL: \(.what) \(expectationDescription)
  expected: \(.valueUnderTest | [expectedValue] | @json)
 but found: \(.valueUnderTest | [  actualValue] | @json)")
    end;

def EXPECT_NE(expectationDescription; actualValue; expectedValue):
    if .valueUnderTest | [actualValue != expectedValue] | all then "PASS: \(expectationDescription)"
    else error("
FAIL: \(.what) \(expectationDescription)
 expected to be other than: \(.valueUnderTest | [expectedValue] | @json)")
    end;

def EXPECT_GT(expectationDescription; actualValue; expectedValue):
    if .valueUnderTest | [actualValue > expectedValue] | all then "PASS: \(expectationDescription)"
    else error("
FAIL: \(.what) \(expectationDescription)
 expected to be greater than: \(.valueUnderTest | [expectedValue] | @json)
                   but found: \(.valueUnderTest | [  actualValue] | @json)")
    end;

def EXPECT_GE(expectationDescription; actualValue; expectedValue):
    if .valueUnderTest | [actualValue >= expectedValue] | all then "PASS: \(expectationDescription)"
    else error("
FAIL: \(.what) \(expectationDescription)
 expected to be >=: \(.valueUnderTest | [expectedValue] | @json)
         but found: \(.valueUnderTest | [  actualValue] | @json)")
    end;

def EXPECT_LE(expectationDescription; actualValue; expectedValue):
    if .valueUnderTest | [actualValue <= expectedValue] | all then "PASS: \(expectationDescription)"
    else error("
FAIL: \(.what) \(expectationDescription)
 expected to be <=: \(.valueUnderTest | [expectedValue] | @json)
         but found: \(.valueUnderTest | [  actualValue] | @json)")
    end;

def EXPECT_LT(expectationDescription; actualValue; expectedValue):
    if .valueUnderTest | [actualValue < expectedValue] | all then "PASS: \(expectationDescription)"
    else error("
FAIL: \(.what) \(expectationDescription)
 expected to be less than: \(.valueUnderTest | [expectedValue] | @json)
                but found: \(.valueUnderTest | [  actualValue] | @json)")
    end;
