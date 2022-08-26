<?php

namespace App\Models;

class ChatImage extends \CodeIgniter\Model
{
    protected $table = 'ChatImage';
    protected $allowedFields = [
        'chat_image_no',
        'chat_no',
        'origin_file_name',
        'stored_file_name',
        'file_size',
        'create_date'
    ];
    protected $primaryKey = 'chat_image_no';
}
