#!/usr/bin/env ruby

ONE_LOCATION=ENV["ONE_LOCATION"]

if !ONE_LOCATION
    RUBY_LIB_LOCATION="/usr/lib/one/ruby"
    LOG_FILE="/var/log/one/beacon.log"
else
    RUBY_LIB_LOCATION=ONE_LOCATION+"/lib/ruby"
    LOG_FILE=ONE_LOCATION+"/var/beacon.log"
end

$: << RUBY_LIB_LOCATION

require 'scripts_common'
require 'opennebula'
include OpenNebula

require 'base64'
require 'nokogiri'
require 'ipaddr'

if ARGV[0].nil?
    OpenNebula.log_error("Missing argument")
    exit -1
end

begin
    client = Client.new()
rescue Exception => e
    OpenNebula.log_error("#{e}")
    exit -1
end

vmxml = Nokogiri::XML(Base64.decode64(ARGV[0]))

# Retrieve the VM owner, get the email from the user template
uid = vmxml.root.at_xpath("UID").text.to_i

user = OpenNebula::User.new_with_id(uid, client)
rc = user.info
exit -1 if OpenNebula.is_error?(rc)

email = user["TEMPLATE/EMAIL"]

if email.nil?
    OpenNebula.log_error("VM owner (uid ##{uid}) does not have an EMAIL attribute in its TEMPLATE")
    exit -1
end

# Call the client for each public IP of this VM
vm_name = vmxml.root.at_xpath("ID").text

File.open(LOG_FILE, 'a'){|f|
    f.puts "#{Time.now} : Processing NICs for VM #{vm_name}" }

vmxml.root.xpath("TEMPLATE/NIC/IP").each { |e|
    addr_info = Addrinfo.getaddrinfo(e.text, nil)

    if addr_info.size > 0 && !addr_info[0].ipv4_private?
        cmd = "java -jar #{File.expand_path(File.dirname(__FILE__))}/Socket.jar '#{vm_name}' '#{e.text}' '#{email}'"

        File.open(LOG_FILE, 'a'){|f|
            f.puts "#{Time.now} : Calling the following command: #{cmd}"

            f.puts ""
            output=`#{cmd} 2>&1 1>>#{LOG_FILE}`
            f.puts output
            f.puts ""
        }
    else
        File.open(LOG_FILE, 'a'){|f|
            f.puts "#{Time.now} : Ignoring IP #{e.text}" }
    end
}

