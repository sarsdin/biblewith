<?php

namespace App\Models;

class GroupBoardImage extends \CodeIgniter\Model
{
    protected $table = 'GroupBoardImage';
    protected $allowedFields = [
        'gboard_image_no',
        'gboard_no',
        'original_file_name',
        'stored_file_name',
        'file_size',
        'create_date'
    ];
    protected $primaryKey = 'gboard_image_no';
}
