package Data;

public class Address implements Comparable<Address> {
    private String file_name;
    private int block_offset;
    private int byte_offset;

    public Address(String file_name, int block_offset, int byte_offset) {
        this.file_name = file_name;
        this.block_offset = block_offset;
        this.byte_offset = byte_offset;
    }

    @Override
    public int compareTo(Address address) {
        if(this.file_name.compareTo(address.file_name) == 0) {
            if(this.block_offset == address.block_offset) {
                return this.byte_offset - address.byte_offset;
            } else {
                return this.block_offset - address.block_offset;
            }
        } else {
            return this.file_name.compareTo(address.file_name);
        }
    }


}
