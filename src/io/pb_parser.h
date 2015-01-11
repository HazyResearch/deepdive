
#include <fcntl.h>
#include <google/protobuf/io/coded_stream.h>
#include <google/protobuf/io/zero_copy_stream.h>
#include <google/protobuf/io/zero_copy_stream_impl.h>

#ifndef _PB_PARSER_H_
#define _PB_PARSER_H_

namespace dd{


  // These two functions are so ugly and should be
  // merged by template... Why not?
  // Working on getting the multinomial working
  // before refactoring...

  template<typename CLASS1, typename CLASS2, void (*handler) (const CLASS1 &, CLASS2 &)>
  long stream_load_pb(std::string filename, CLASS2 & fg){
    GOOGLE_PROTOBUF_VERIFY_VERSION;
    CLASS1 tmp;
    long ct = 0;
    int fd = open(filename.c_str(), O_RDONLY);
    google::protobuf::io::ZeroCopyInputStream* raw_input 
                  = new google::protobuf::io::FileInputStream(fd);
    google::protobuf::io::CodedInputStream * coded_input 
                  = new google::protobuf::io::CodedInputStream(raw_input);
    google::protobuf::uint32 bytes;
    google::protobuf::io::CodedInputStream::Limit msgLimit;
    coded_input->SetTotalBytesLimit(2e9, 9e8);
    while(coded_input->ReadVarint32(&bytes)){      
      //std::cout << bytes << std::endl;
      //assert(bytes > 0);
      msgLimit = coded_input->PushLimit(bytes);
      //if(a)
      //  std::cout << "~~~" << bytes << std::endl;
      //coded_input->Skip(3);
      
      if(tmp.ParseFromCodedStream(coded_input)){
        fg.tmp = bytes;
        handler(tmp, fg);
        ct ++;
      }else{
        std::cout << "[ERROR] Oops! Seems that your .proto is wrong or too large? " 
                  << filename << std::endl;
        std::cout << "[ERROR] Send the file to czhang@cs.wisc.edu..." << std::endl; 
        assert(false);
      }
      
      coded_input->PopLimit(msgLimit);
    }
    google::protobuf::ShutdownProtobufLibrary();
    return ct;
  }


  template<typename CLASS1, typename CLASS2, void (*handler) (const CLASS1 &, CLASS2 &)>
  long single_load_pb(std::string filename, CLASS2 & fg){
    GOOGLE_PROTOBUF_VERIFY_VERSION;
    CLASS1 tmp;
    long ct = 0;
    int fd = open(filename.c_str(), O_RDONLY);
    google::protobuf::io::ZeroCopyInputStream* raw_input 
                  = new google::protobuf::io::FileInputStream(fd);
    google::protobuf::io::CodedInputStream * coded_input 
                  = new google::protobuf::io::CodedInputStream(raw_input);
    google::protobuf::uint32 bytes;
    google::protobuf::io::CodedInputStream::Limit msgLimit;
    coded_input->SetTotalBytesLimit(1e9, 9e8);
    while(true){
      if(tmp.ParseFromCodedStream(coded_input)){
        handler(tmp, fg);
        ct ++;
      }else{
        std::cout << "[ERROR] Oops! Seems that your .proto is wrong or too large? " 
                  << filename << std::endl;
        std::cout << "[ERROR] Send the file to czhang@cs.wisc.edu..." << std::endl; 
        assert(false);
      }
      break;
      coded_input->PopLimit(msgLimit);
    }
    google::protobuf::ShutdownProtobufLibrary();
    return ct;
  }


  template<typename CLASS1>
  CLASS1 read_single_pb(std::string filename){
    GOOGLE_PROTOBUF_VERIFY_VERSION;
    CLASS1 tmp;
    long ct = 0;
    int fd = open(filename.c_str(), O_RDONLY);
    google::protobuf::io::ZeroCopyInputStream* raw_input 
                  = new google::protobuf::io::FileInputStream(fd);
    google::protobuf::io::CodedInputStream * coded_input 
                  = new google::protobuf::io::CodedInputStream(raw_input);
    google::protobuf::uint32 bytes;
    google::protobuf::io::CodedInputStream::Limit msgLimit;
    coded_input->SetTotalBytesLimit(1e9, 9e8);
    while(true){
      if(tmp.ParseFromCodedStream(coded_input)){
        return tmp;
        ct ++;
      }else{
        std::cout << "[ERROR] Oops! Seems that your .proto is wrong or too large? " 
                  << filename << std::endl;
        std::cout << "[ERROR] Send the file to czhang@cs.wisc.edu..." << std::endl; 
        assert(false);
      }
      break;
      coded_input->PopLimit(msgLimit);
    }
    google::protobuf::ShutdownProtobufLibrary();
    assert(false);
  }


}

#endif
