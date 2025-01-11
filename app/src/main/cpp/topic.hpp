#ifndef MULTICAMERADEMO_TOPIC_HPP
#define MULTICAMERADEMO_TOPIC_HPP

#define TOPIC_MARKING          "MA"
#define TOPIC_RECORD_SWITCH    "MB"
#define TOPIC_MUTE_SWITCH      "MC"
#define TOPIC_PHONE_POWER      "MD"
#define TOPIC_REMOTE_INFO_STATE "ME"
#define TOPIC_REMOTE_CTRL_STATE "MF"
#define TOPIC_SYNCHRONIZE_SWITCH "MG"
#define TOPIC_CAPTURED_SWITCH  "MH"
#define TOPIC_SCOREBOARD_INFO  "MI"

class Value {
public:
    virtual ~Value() = default;
    virtual std::string getType() const = 0;
    virtual std::string getStringValue() const { return ""; }
    virtual int getIntValue() const { return 0; }
};

class StringValue : public Value {
public:
    explicit StringValue(std::string  value) : value_(std::move(value)) {}
    std::string getType() const override { return "string"; }
    std::string getStringValue() const override { return value_; }
private:
    std::string value_;
};

class IntValue : public Value {
public:
    explicit IntValue(int value) : value_(value) {}
    std::string getType() const override { return "int"; }
    int getIntValue() const override { return value_; }
private:
    int value_;
};


#endif //MULTICAMERADEMO_TOPIC_HPP
